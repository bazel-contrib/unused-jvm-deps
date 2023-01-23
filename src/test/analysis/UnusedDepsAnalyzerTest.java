package com.stripe.build.dependencyanalyzer.analysis;

import com.stripe.build.dependencyanalyzer.database.Database;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

@RunWith(JUnit4.class)
public class UnusedDepsAnalyzerTest {

    private File dbFile;
    private Database db;

    @Before
    public void setup() throws IOException, SQLException {
        // TODO: 1. Setup a database
        //       2. Create a third-party-index
        //       3. Define a target filter

        dbFile = File.createTempFile("unused-deps-test-db", null);
        db = Database.createNew(dbFile.toPath());
    }

    @After
    public void tearDown() throws SQLException {
        db.close();
        assert dbFile.delete();
    }

    @Test
    public void TestExcludedDeps() {
        // Setup database
    }
}
