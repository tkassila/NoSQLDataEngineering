package es.um.nosql.orchestrator.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;

import es.um.nosql.s13e.NoSQLSchema.NoSQLSchema;
import es.um.nosql.s13e.db.interfaces.EveryPolitician2Db;
import es.um.nosql.s13e.db.util.DbType;
import es.um.nosql.s13e.json2dbschema.main.BuildNoSQLSchema;
import es.um.nosql.s13e.nosqlimport.db.mongodb.MongoDBImport;
import es.um.nosql.s13e.util.NoSQLSchemaPrettyPrinter;

public class MergeEVTest
{
  String inputRoute = "testSources/ERROR_MergeEV.json";
  String dbName = "DEBUG_everypolitician";
  EveryPolitician2Db controller;

  @Before
  public void setUp() throws Exception
  {
    controller = new EveryPolitician2Db(DbType.MONGODB, "localhost");
  }

  @After
  public void tearDown() throws Exception
  {
    controller.getClient().cleanDb(dbName);
    controller.shutdown();
  }

  @Test
  public void test()
  {
    controller.run(inputRoute, dbName);
    controller.shutdown();

    MongoDBImport inferrer = new MongoDBImport();
    JsonArray jArray = inferrer.mapRed2Array("localhost", dbName, "mapreduce/mongodb/v1/");

    BuildNoSQLSchema builder = new BuildNoSQLSchema();
    NoSQLSchema nosqlschema = builder.buildFromGsonArray(dbName, jArray);
    System.out.println(NoSQLSchemaPrettyPrinter.printPretty(nosqlschema));

    Assert.assertEquals(nosqlschema.getEntities(), 2);
    Assert.assertEquals(nosqlschema.getEntities().get(0), 1);
    Assert.assertEquals(nosqlschema.getEntities().get(1), 2);
  }
}