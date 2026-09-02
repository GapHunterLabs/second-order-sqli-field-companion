package dev.gaphunter.secondordersqlifieldcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.secondordersqlifieldcompanion.model.EntityFieldKey

/**
 * Finds a WRITE site: `entity.setFieldName(ARG)` (bean-setter
 * convention) or `entity.fieldName = ARG` (direct assignment), where
 * `entity`'s declared type is a KNOWN `@Entity` class (from
 * [entityStringFields]) and `ARG` is tainted (a bare reference or
 * `+`-concatenation containing one of [paramNames], the containing
 * endpoint method's own parameters).
 */
object WriteSiteScanner {

    fun findWriteSites(body: PsiCodeBlock, paramNames: List<String>, entityStringFields: Map<String, Set<String>>): Set<EntityFieldKey> {
        val result = mutableSetOf<EntityFieldKey>()

        body.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                super.visitMethodCallExpression(call)
                val methodName = call.methodExpression.referenceName ?: return
                val fieldName = fieldNameFromAccessor(methodName, "set") ?: return
                val qualifier = call.methodExpression.qualifierExpression ?: return
                val className = (qualifier.type as? PsiClassType)?.className ?: return
                if (fieldName !in entityStringFields[className].orEmpty()) return

                val argument = call.argumentList.expressions.getOrNull(0) ?: return
                if (paramNames.any { TaintReferenceMatcher.isTaintedReference(argument, it) }) {
                    result += EntityFieldKey(className, fieldName)
                }
            }

            override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
                super.visitAssignmentExpression(expression)
                val lhs = expression.lExpression as? PsiReferenceExpression ?: return
                val fieldName = lhs.referenceName ?: return
                val qualifier = lhs.qualifierExpression ?: return
                val className = (qualifier.type as? PsiClassType)?.className ?: return
                if (fieldName !in entityStringFields[className].orEmpty()) return

                val rhs = expression.rExpression ?: return
                if (paramNames.any { TaintReferenceMatcher.isTaintedReference(rhs, it) }) {
                    result += EntityFieldKey(className, fieldName)
                }
            }
        })
        return result
    }

    /** `"setName"` + prefix `"set"` -> `"name"` (bean-property decapitalization); null when [methodName] doesn't start with [prefix] or has nothing after it. */
    fun fieldNameFromAccessor(methodName: String, prefix: String): String? {
        if (!methodName.startsWith(prefix) || methodName.length <= prefix.length) return null
        val rest = methodName.substring(prefix.length)
        return rest[0].lowercaseChar() + rest.substring(1)
    }
}
