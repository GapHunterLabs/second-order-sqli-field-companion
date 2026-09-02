package dev.gaphunter.secondordersqlifieldcompanion.detect

import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression

/** Whether [expression] is a BARE reference to [targetName], or a `+` concatenation where at least one operand is -- same "direct reference or one-hop concatenation" shape this catalog's other sink finders use. Any wrapping method call breaks the chain (never descended into). */
object TaintReferenceMatcher {

    fun isTaintedReference(expression: PsiExpression, targetName: String): Boolean = when (expression) {
        is PsiReferenceExpression -> expression.referenceName == targetName && expression.qualifierExpression == null
        is PsiPolyadicExpression -> expression.operationTokenType == JavaTokenType.PLUS &&
            expression.operands.any { isTaintedReference(it, targetName) }
        else -> false
    }
}
