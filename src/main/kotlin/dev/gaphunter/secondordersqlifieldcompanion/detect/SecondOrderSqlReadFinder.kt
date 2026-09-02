package dev.gaphunter.secondordersqlifieldcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.secondordersqlifieldcompanion.model.EntityFieldKey
import dev.gaphunter.secondordersqlifieldcompanion.model.SecondOrderSqlHit

/**
 * Finds a READ site: a JDBC/JPA sink call
 * (`executeQuery`/`executeUpdate`/`execute`/`createNativeQuery`/
 * `createQuery`) whose first argument is a `+`-concatenation
 * containing `entity.getFieldName()` (bean-getter convention) or a
 * direct `entity.fieldName` read, where the field is a KNOWN `@Entity`
 * `String` field that [ProjectSecondOrderSqlAnalyzer] has ALSO
 * confirmed a write site for -- the second-order injection.
 */
object SecondOrderSqlReadFinder {

    private val SINK_METHOD_NAMES = setOf("executeQuery", "executeUpdate", "execute", "createNativeQuery", "createQuery")

    fun findAll(file: PsiFile, facts: ProjectSecondOrderSqlAnalyzer.ProjectFacts): List<SecondOrderSqlHit> {
        if (facts.writeSites.isEmpty()) return emptyList()

        val hits = mutableListOf<SecondOrderSqlHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                super.visitMethodCallExpression(call)
                if (call.methodExpression.referenceName !in SINK_METHOD_NAMES) return
                val argument = call.argumentList.expressions.getOrNull(0) as? PsiPolyadicExpression ?: return
                if (argument.operationTokenType != JavaTokenType.PLUS) return

                for (operand in argument.operands) {
                    val key = entityFieldReadKey(operand, facts.entityStringFields) ?: continue
                    if (key in facts.writeSites) {
                        hits += SecondOrderSqlHit(call.methodExpression.referenceNameElement ?: call.methodExpression, key)
                        break // one finding per sink call site is enough
                    }
                }
            }
        })
        return hits
    }

    private fun entityFieldReadKey(expression: PsiExpression, entityStringFields: Map<String, Set<String>>): EntityFieldKey? {
        if (expression is PsiMethodCallExpression) {
            val methodName = expression.methodExpression.referenceName ?: return null
            val fieldName = WriteSiteScanner.fieldNameFromAccessor(methodName, "get") ?: return null
            val qualifier = expression.methodExpression.qualifierExpression ?: return null
            val className = (qualifier.type as? PsiClassType)?.className ?: return null
            return if (fieldName in entityStringFields[className].orEmpty()) EntityFieldKey(className, fieldName) else null
        }
        if (expression is PsiReferenceExpression) {
            val fieldName = expression.referenceName ?: return null
            val qualifier = expression.qualifierExpression ?: return null
            val className = (qualifier.type as? PsiClassType)?.className ?: return null
            return if (fieldName in entityStringFields[className].orEmpty()) EntityFieldKey(className, fieldName) else null
        }
        return null
    }
}
