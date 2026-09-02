package dev.gaphunter.secondordersqlifieldcompanion.detect

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import dev.gaphunter.secondordersqlifieldcompanion.model.EntityFieldKey

/**
 * Real field-sensitive, two-phase, whole-project points-to analysis --
 * the correlation is by FIELD NAME across two call sites that may
 * share no call-graph relationship whatsoever (the "call" connecting
 * them is conceptual, through a real database between the write and
 * the read, invisible to PSI): (1) catalogs every `@Entity` class's
 * own `String` fields; (2) finds a WRITE site
 * ([WriteSiteScanner]) -- a field set from a tainted HTTP endpoint
 * parameter, ANYWHERE in the project; a separate, per-file pass (see
 * [SecondOrderSqlReadFinder]) finds a READ site -- that SAME field
 * flowing into a concatenated SQL sink, possibly in a completely
 * unrelated class. This is exactly the shape documented as the real
 * blind spot of simple pattern-matching SAST tools (second-order SQL
 * injection: "malicious data is stored and later used in a different
 * execution context").
 *
 * Cached per-project via [CachedValuesManager].
 *
 * **v0.1 scope, stated honestly:** the field name must be textually
 * IDENTICAL between write and read (never resolves a field alias);
 * only `String`-typed fields of a class textually annotated `@Entity`;
 * a write site is only recognized within an HTTP endpoint method's own
 * body, tainted by that SAME method's own parameter (same "direct
 * reference or one-hop concatenation" taint shape this catalog's other
 * sink finders use); a project with more than [MAX_FILES] `.java`
 * files skips analysis entirely.
 */
object ProjectSecondOrderSqlAnalyzer {

    const val MAX_FILES = 2000
    private const val MAX_FILE_LENGTH = 500_000

    private val CACHE_KEY: Key<CachedValue<ProjectFacts>> = Key.create("secondOrderSqliFieldCompanion.facts")

    class ProjectFacts(
        val entityStringFields: Map<String, Set<String>>,
        val writeSites: Set<EntityFieldKey>,
    )

    fun factsFor(project: Project): ProjectFacts {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE_KEY,
            { CachedValueProvider.Result.create(computeFacts(project), PsiModificationTracker.MODIFICATION_COUNT) },
            false,
        )
    }

    private fun computeFacts(project: Project): ProjectFacts {
        val scope = GlobalSearchScope.projectScope(project)
        val files = FilenameIndex.getAllFilesByExt(project, "java", scope)
        if (files.size > MAX_FILES) return ProjectFacts(emptyMap(), emptySet())

        val psiManager = PsiManager.getInstance(project)
        val javaFiles = files.mapNotNull { psiManager.findFile(it) as? PsiJavaFile }
            .filter { it.text.length <= MAX_FILE_LENGTH }

        val entityStringFields = mutableMapOf<String, MutableSet<String>>()
        for (psiFile in javaFiles) {
            psiFile.accept(object : JavaRecursiveElementWalkingVisitor() {
                override fun visitClass(psiClass: PsiClass) {
                    super.visitClass(psiClass)
                    if (!isEntityAnnotated(psiClass)) return
                    val className = psiClass.name ?: return
                    val stringFieldNames = psiClass.fields
                        .filter { (it.type as? PsiClassType)?.className == "String" }
                        .mapNotNull { it.name }
                    if (stringFieldNames.isNotEmpty()) {
                        entityStringFields.getOrPut(className) { mutableSetOf() }.addAll(stringFieldNames)
                    }
                }
            })
        }

        val writeSites = mutableSetOf<EntityFieldKey>()
        for (psiFile in javaFiles) {
            psiFile.accept(object : JavaRecursiveElementWalkingVisitor() {
                override fun visitMethod(method: PsiMethod) {
                    super.visitMethod(method)
                    if (!ControllerEndpointSignals.isEndpointMethod(method)) return
                    val body = method.body ?: return
                    val paramNames = method.parameterList.parameters.map { it.name }
                    writeSites += WriteSiteScanner.findWriteSites(body, paramNames, entityStringFields)
                }
            })
        }

        return ProjectFacts(entityStringFields, writeSites)
    }

    private fun isEntityAnnotated(psiClass: PsiClass): Boolean =
        psiClass.annotations.any { it.nameReferenceElement?.referenceName == "Entity" }
}
