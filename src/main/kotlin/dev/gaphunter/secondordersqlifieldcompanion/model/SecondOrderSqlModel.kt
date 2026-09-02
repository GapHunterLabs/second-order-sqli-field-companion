package dev.gaphunter.secondordersqlifieldcompanion.model

import com.intellij.psi.PsiElement

/** A `@Entity` class's simple name paired with one of its own String field names -- the correlation key linking a write site (tainted input stored into the field) to a read site (the field's value later reaching a SQL sink). */
data class EntityFieldKey(val entityClassName: String, val fieldName: String)

/** A confirmed second-order SQL injection: [key] was written from tainted input SOMEWHERE in the project, and is read into a concatenated SQL sink at [anchor]. */
data class SecondOrderSqlHit(val anchor: PsiElement, val key: EntityFieldKey)
