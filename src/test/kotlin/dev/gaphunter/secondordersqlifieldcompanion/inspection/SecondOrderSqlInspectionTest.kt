package dev.gaphunter.secondordersqlifieldcompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Every test method uses its own uniquely-suffixed entity/field names, same discipline as this catalog's other cross-file test suites. */
class SecondOrderSqlInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SecondOrderSqlInspection::class.java)
    }

    fun `test a field written from tainted input in one file and read into SQL in another is flagged`() {
        myFixture.addFileToProject(
            "UserRecordA.java",
            """
            import javax.persistence.Entity;

            @Entity
            class UserRecordA {
                private String usernameA;
                public String getUsernameA() { return usernameA; }
                public void setUsernameA(String usernameA) { this.usernameA = usernameA; }
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "UserControllerA.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            class UserControllerA {
                @PostMapping("/user")
                void create(String usernameA) {
                    UserRecordA record = new UserRecordA();
                    record.setUsernameA(usernameA);
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "UserRepositoryA.java",
            """
            import java.sql.Statement;

            class UserRepositoryA {
                void find(Statement stmt, UserRecordA record) throws Exception {
                    stmt.executeQuery("SELECT * FROM users WHERE name = '" + record.getUsernameA() + "'");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("CWE-89") == true && it.description?.contains("UserRecordA.usernameA") == true })
    }

    fun `test a read with no corresponding write anywhere is not flagged`() {
        myFixture.addFileToProject(
            "UserRecordB.java",
            """
            import javax.persistence.Entity;

            @Entity
            class UserRecordB {
                private String usernameB;
                public String getUsernameB() { return usernameB; }
                public void setUsernameB(String usernameB) { this.usernameB = usernameB; }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "UserRepositoryB.java",
            """
            import java.sql.Statement;

            class UserRepositoryB {
                void find(Statement stmt, UserRecordB record) throws Exception {
                    stmt.executeQuery("SELECT * FROM users WHERE name = '" + record.getUsernameB() + "'");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-89") == true })
    }

    fun `test a write to a DIFFERENT field than the one read is not flagged`() {
        myFixture.addFileToProject(
            "UserRecordC.java",
            """
            import javax.persistence.Entity;

            @Entity
            class UserRecordC {
                private String usernameC;
                private String emailC;
                public String getUsernameC() { return usernameC; }
                public void setUsernameC(String usernameC) { this.usernameC = usernameC; }
                public String getEmailC() { return emailC; }
                public void setEmailC(String emailC) { this.emailC = emailC; }
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "UserControllerC.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            class UserControllerC {
                @PostMapping("/user")
                void create(String emailC) {
                    UserRecordC record = new UserRecordC();
                    record.setEmailC(emailC);
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "UserRepositoryC.java",
            """
            import java.sql.Statement;

            class UserRepositoryC {
                void find(Statement stmt, UserRecordC record) throws Exception {
                    stmt.executeQuery("SELECT * FROM users WHERE name = '" + record.getUsernameC() + "'");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-89") == true })
    }

    fun `test a direct field assignment write site is also recognized`() {
        myFixture.addFileToProject(
            "UserRecordD.java",
            """
            import javax.persistence.Entity;

            @Entity
            class UserRecordD {
                public String usernameD;
                public String getUsernameD() { return usernameD; }
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "UserControllerD.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            class UserControllerD {
                @PostMapping("/user")
                void create(String usernameD) {
                    UserRecordD record = new UserRecordD();
                    record.usernameD = usernameD;
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "UserRepositoryD.java",
            """
            import java.sql.Statement;

            class UserRepositoryD {
                void find(Statement stmt, UserRecordD record) throws Exception {
                    stmt.executeQuery("SELECT * FROM users WHERE name = '" + record.getUsernameD() + "'");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("CWE-89") == true })
    }

    fun `test a write wrapped in a non-bare call is not flagged`() {
        myFixture.addFileToProject(
            "UserRecordE.java",
            """
            import javax.persistence.Entity;

            @Entity
            class UserRecordE {
                private String usernameE;
                public String getUsernameE() { return usernameE; }
                public void setUsernameE(String usernameE) { this.usernameE = usernameE; }
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "UserControllerE.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            class UserControllerE {
                @PostMapping("/user")
                void create(String usernameE) {
                    UserRecordE record = new UserRecordE();
                    record.setUsernameE(usernameE.trim());
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "UserRepositoryE.java",
            """
            import java.sql.Statement;

            class UserRepositoryE {
                void find(Statement stmt, UserRecordE record) throws Exception {
                    stmt.executeQuery("SELECT * FROM users WHERE name = '" + record.getUsernameE() + "'");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-89") == true })
    }
}
