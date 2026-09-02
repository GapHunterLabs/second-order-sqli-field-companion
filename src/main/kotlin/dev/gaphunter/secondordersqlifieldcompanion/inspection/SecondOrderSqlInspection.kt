package dev.gaphunter.secondordersqlifieldcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import dev.gaphunter.secondordersqlifieldcompanion.detect.ProjectSecondOrderSqlAnalyzer
import dev.gaphunter.secondordersqlifieldcompanion.detect.SecondOrderSqlReadFinder
import dev.gaphunter.secondordersqlifieldcompanion.model.SecondOrderSqlHit
import dev.gaphunter.secondordersqlifieldcompanion.review.ReviewPrompt

/** Flags a SQL sink reading a `@Entity` field that was ALSO confirmed, elsewhere in the project, to be written from tainted HTTP input -- CWE-89, the second-order form. See [SecondOrderSqlReadFinder]. */
class SecondOrderSqlInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null

        val facts = ProjectSecondOrderSqlAnalyzer.factsFor(file.project)
        val hits = SecondOrderSqlReadFinder.findAll(file, facts)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber:${hit.key.entityClassName}.${hit.key.fieldName}")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: SecondOrderSqlHit): String =
        "'${hit.key.entityClassName}.${hit.key.fieldName}' is concatenated into this SQL query, but that field is ALSO written " +
            "from unsanitized HTTP input elsewhere in the project -- second-order SQL injection (CWE-89): the tainted value passes " +
            "through persistent storage before reaching this sink, invisible to a single-method or single-call-chain taint check"
}
