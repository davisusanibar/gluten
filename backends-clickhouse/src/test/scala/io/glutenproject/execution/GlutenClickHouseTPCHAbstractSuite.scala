/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.glutenproject.execution

import io.glutenproject.GlutenConfig
import io.glutenproject.utils.UTSystemParameters

import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.execution.datasources.v2.clickhouse.ClickHouseLog

import org.apache.commons.io.FileUtils

import java.io.File

abstract class GlutenClickHouseTPCHAbstractSuite extends WholeStageTransformerSuite with Logging {

  protected val createNullableTables = false

  override protected val backend: String = "ch"
  override protected val resourcePath: String = "tpch-data-ch"
  override protected val fileFormat: String = "parquet"

  protected val rootPath: String = getClass.getResource("/").getPath
  protected val basePath: String = rootPath + "unit-tests-working-home"

  protected val warehouse: String = basePath + "/spark-warehouse"
  protected val metaStorePathAbsolute: String = basePath + "/meta"

  protected val tablesPath: String
  protected val tpchQueries: String
  protected val queriesResults: String

  override def beforeAll(): Unit = {
    // prepare working paths
    val basePathDir = new File(basePath)
    if (basePathDir.exists()) {
      FileUtils.forceDelete(basePathDir)
    }
    FileUtils.forceMkdir(basePathDir)
    FileUtils.forceMkdir(new File(warehouse))
    FileUtils.forceMkdir(new File(metaStorePathAbsolute))
    FileUtils.copyDirectory(new File(rootPath + resourcePath), new File(tablesPath))
    super.beforeAll()
    spark.sparkContext.setLogLevel("WARN")
    if (createNullableTables) {
      createTPCHNullableTables()
    } else {
      createTPCHNotNullTables()
    }
  }

  override protected def createTPCHNotNullTables(): Unit = {
    val customerData = tablesPath + "/customer"
    spark.sql(s"DROP TABLE IF EXISTS customer")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS customer (
                 | c_custkey    bigint not null,
                 | c_name       string not null,
                 | c_address    string not null,
                 | c_nationkey  bigint not null,
                 | c_phone      string not null,
                 | c_acctbal    double not null,
                 | c_mktsegment string not null,
                 | c_comment    string not null)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$customerData'
                 |""".stripMargin)

    val lineitemData = tablesPath + "/lineitem"
    spark.sql(s"DROP TABLE IF EXISTS lineitem")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS lineitem (
                 | l_orderkey      bigint not null,
                 | l_partkey       bigint not null,
                 | l_suppkey       bigint not null,
                 | l_linenumber    bigint not null,
                 | l_quantity      double not null,
                 | l_extendedprice double not null,
                 | l_discount      double not null,
                 | l_tax           double not null,
                 | l_returnflag    string not null,
                 | l_linestatus    string not null,
                 | l_shipdate      date not null,
                 | l_commitdate    date not null,
                 | l_receiptdate   date not null,
                 | l_shipinstruct  string not null,
                 | l_shipmode      string not null,
                 | l_comment       string not null)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$lineitemData'
                 |""".stripMargin)

    val nationData = tablesPath + "/nation"
    spark.sql(s"DROP TABLE IF EXISTS nation")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS nation (
                 | n_nationkey bigint not null,
                 | n_name      string not null,
                 | n_regionkey bigint not null,
                 | n_comment   string not null)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$nationData'
                 |""".stripMargin)

    val regionData = tablesPath + "/region"
    spark.sql(s"DROP TABLE IF EXISTS region")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS region (
                 | r_regionkey bigint not null,
                 | r_name      string not null,
                 | r_comment   string not null)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$regionData'
                 |""".stripMargin)

    val ordersData = tablesPath + "/orders"
    spark.sql(s"DROP TABLE IF EXISTS orders")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS orders (
                 | o_orderkey      bigint not null,
                 | o_custkey       bigint not null,
                 | o_orderstatus   string not null,
                 | o_totalprice    double not null,
                 | o_orderdate     date not null,
                 | o_orderpriority string not null,
                 | o_clerk         string not null,
                 | o_shippriority  bigint not null,
                 | o_comment       string not null)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$ordersData'
                 |""".stripMargin)

    val partData = tablesPath + "/part"
    spark.sql(s"DROP TABLE IF EXISTS part")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS part (
                 | p_partkey     bigint not null,
                 | p_name        string not null,
                 | p_mfgr        string not null,
                 | p_brand       string not null,
                 | p_type        string not null,
                 | p_size        bigint not null,
                 | p_container   string not null,
                 | p_retailprice double not null,
                 | p_comment     string not null)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$partData'
                 |""".stripMargin)

    val partsuppData = tablesPath + "/partsupp"
    spark.sql(s"DROP TABLE IF EXISTS partsupp")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS partsupp (
                 | ps_partkey    bigint not null,
                 | ps_suppkey    bigint not null,
                 | ps_availqty   bigint not null,
                 | ps_supplycost double not null,
                 | ps_comment    string not null)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$partsuppData'
                 |""".stripMargin)

    val supplierData = tablesPath + "/supplier"
    spark.sql(s"DROP TABLE IF EXISTS supplier")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS supplier (
                 | s_suppkey   bigint not null,
                 | s_name      string not null,
                 | s_address   string not null,
                 | s_nationkey bigint not null,
                 | s_phone     string not null,
                 | s_acctbal   double not null,
                 | s_comment   string not null)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$supplierData'
                 |""".stripMargin)

    val result = spark
      .sql(s"""
              | show tables;
              |""".stripMargin)
      .collect()
    assert(result.size == 8)
  }

  protected def createTPCHNullableTables(): Unit = {
    spark.sql(s"""
                 |CREATE DATABASE IF NOT EXISTS tpch_nullable
                 |""".stripMargin)
    spark.sql("use tpch_nullable")
    val customerData = tablesPath + "/customer"
    spark.sql(s"DROP TABLE IF EXISTS customer")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS customer (
                 | c_custkey    bigint,
                 | c_name       string,
                 | c_address    string,
                 | c_nationkey  bigint,
                 | c_phone      string,
                 | c_acctbal    double,
                 | c_mktsegment string,
                 | c_comment    string)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$customerData'
                 |""".stripMargin)

    val lineitemData = tablesPath + "/lineitem"
    spark.sql(s"DROP TABLE IF EXISTS lineitem")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS lineitem (
                 | l_orderkey      bigint,
                 | l_partkey       bigint,
                 | l_suppkey       bigint,
                 | l_linenumber    bigint,
                 | l_quantity      double,
                 | l_extendedprice double,
                 | l_discount      double,
                 | l_tax           double,
                 | l_returnflag    string,
                 | l_linestatus    string,
                 | l_shipdate      date,
                 | l_commitdate    date,
                 | l_receiptdate   date,
                 | l_shipinstruct  string,
                 | l_shipmode      string,
                 | l_comment       string)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$lineitemData'
                 |""".stripMargin)

    val nationData = tablesPath + "/nation"
    spark.sql(s"DROP TABLE IF EXISTS nation")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS nation (
                 | n_nationkey bigint,
                 | n_name      string,
                 | n_regionkey bigint,
                 | n_comment   string)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$nationData'
                 |""".stripMargin)

    val regionData = tablesPath + "/region"
    spark.sql(s"DROP TABLE IF EXISTS region")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS region (
                 | r_regionkey bigint,
                 | r_name      string,
                 | r_comment   string)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$regionData'
                 |""".stripMargin)

    val ordersData = tablesPath + "/orders"
    spark.sql(s"DROP TABLE IF EXISTS orders")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS orders (
                 | o_orderkey      bigint,
                 | o_custkey       bigint,
                 | o_orderstatus   string,
                 | o_totalprice    double,
                 | o_orderdate     date,
                 | o_orderpriority string,
                 | o_clerk         string,
                 | o_shippriority  bigint,
                 | o_comment       string)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$ordersData'
                 |""".stripMargin)

    val partData = tablesPath + "/part"
    spark.sql(s"DROP TABLE IF EXISTS part")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS part (
                 | p_partkey     bigint,
                 | p_name        string,
                 | p_mfgr        string,
                 | p_brand       string,
                 | p_type        string,
                 | p_size        bigint,
                 | p_container   string,
                 | p_retailprice double,
                 | p_comment     string)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$partData'
                 |""".stripMargin)

    val partsuppData = tablesPath + "/partsupp"
    spark.sql(s"DROP TABLE IF EXISTS partsupp")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS partsupp (
                 | ps_partkey    bigint,
                 | ps_suppkey    bigint,
                 | ps_availqty   bigint,
                 | ps_supplycost double,
                 | ps_comment    string)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$partsuppData'
                 |""".stripMargin)

    val supplierData = tablesPath + "/supplier"
    spark.sql(s"DROP TABLE IF EXISTS supplier")
    spark.sql(s"""
                 | CREATE EXTERNAL TABLE IF NOT EXISTS supplier (
                 | s_suppkey   bigint,
                 | s_name      string,
                 | s_address   string,
                 | s_nationkey bigint,
                 | s_phone     string,
                 | s_acctbal   double,
                 | s_comment   string)
                 | USING clickhouse
                 | TBLPROPERTIES (engine='MergeTree'
                 |                )
                 | LOCATION '$supplierData'
                 |""".stripMargin)

    val result = spark
      .sql(s"""
              | show tables;
              |""".stripMargin)
      .collect()
    assert(result.size == 8)
  }

  protected def createTPCHParquetTables(parquetTablePath: String): Unit = {
    val customerData = parquetTablePath + "/customer"
    spark.sql(s"DROP TABLE IF EXISTS customer")
    spark.sql(s"""
                 | CREATE TABLE IF NOT EXISTS customer (
                 | c_custkey    bigint,
                 | c_name       string,
                 | c_address    string,
                 | c_nationkey  bigint,
                 | c_phone      string,
                 | c_acctbal    double,
                 | c_mktsegment string,
                 | c_comment    string)
                 | USING PARQUET LOCATION '$customerData'
                 |""".stripMargin)

    val lineitemData = parquetTablePath + "/lineitem"
    spark.sql(s"DROP TABLE IF EXISTS lineitem")
    spark.sql(s"""
                 | CREATE TABLE IF NOT EXISTS lineitem (
                 | l_orderkey      bigint,
                 | l_partkey       bigint,
                 | l_suppkey       bigint,
                 | l_linenumber    bigint,
                 | l_quantity      double,
                 | l_extendedprice double,
                 | l_discount      double,
                 | l_tax           double,
                 | l_returnflag    string,
                 | l_linestatus    string,
                 | l_shipdate      date,
                 | l_commitdate    date,
                 | l_receiptdate   date,
                 | l_shipinstruct  string,
                 | l_shipmode      string,
                 | l_comment       string)
                 | USING PARQUET LOCATION '$lineitemData'
                 |""".stripMargin)

    val nationData = parquetTablePath + "/nation"
    spark.sql(s"DROP TABLE IF EXISTS nation")
    spark.sql(s"""
                 | CREATE TABLE IF NOT EXISTS nation (
                 | n_nationkey bigint,
                 | n_name      string,
                 | n_regionkey bigint,
                 | n_comment   string)
                 | USING PARQUET LOCATION '$nationData'
                 |""".stripMargin)

    val regionData = parquetTablePath + "/region"
    spark.sql(s"DROP TABLE IF EXISTS region")
    spark.sql(s"""
                 | CREATE TABLE IF NOT EXISTS region (
                 | r_regionkey bigint,
                 | r_name      string,
                 | r_comment   string)
                 | USING PARQUET LOCATION '$regionData'
                 |""".stripMargin)

    val ordersData = parquetTablePath + "/orders"
    spark.sql(s"DROP TABLE IF EXISTS orders")
    spark.sql(s"""
                 | CREATE TABLE IF NOT EXISTS orders (
                 | o_orderkey      bigint,
                 | o_custkey       bigint,
                 | o_orderstatus   string,
                 | o_totalprice    double,
                 | o_orderdate     date,
                 | o_orderpriority string,
                 | o_clerk         string,
                 | o_shippriority  bigint,
                 | o_comment       string)
                 | USING PARQUET LOCATION '$ordersData'
                 |""".stripMargin)

    val partData = parquetTablePath + "/part"
    spark.sql(s"DROP TABLE IF EXISTS part")
    spark.sql(s"""
                 | CREATE TABLE IF NOT EXISTS part (
                 | p_partkey     bigint,
                 | p_name        string,
                 | p_mfgr        string,
                 | p_brand       string,
                 | p_type        string,
                 | p_size        bigint,
                 | p_container   string,
                 | p_retailprice double,
                 | p_comment     string)
                 | USING PARQUET LOCATION '$partData'
                 |""".stripMargin)

    val partsuppData = parquetTablePath + "/partsupp"
    spark.sql(s"DROP TABLE IF EXISTS partsupp")
    spark.sql(s"""
                 | CREATE TABLE IF NOT EXISTS partsupp (
                 | ps_partkey    bigint,
                 | ps_suppkey    bigint,
                 | ps_availqty   bigint,
                 | ps_supplycost double,
                 | ps_comment    string)
                 | USING PARQUET LOCATION '$partsuppData'
                 |""".stripMargin)

    val supplierData = parquetTablePath + "/supplier"
    spark.sql(s"DROP TABLE IF EXISTS supplier")
    spark.sql(s"""
                 | CREATE TABLE IF NOT EXISTS supplier (
                 | s_suppkey   bigint,
                 | s_name      string,
                 | s_address   string,
                 | s_nationkey bigint,
                 | s_phone     string,
                 | s_acctbal   double,
                 | s_comment   string)
                 | USING PARQUET LOCATION '$supplierData'
                 |""".stripMargin)

    val result = spark
      .sql(s"""
              | show tables;
              |""".stripMargin)
      .collect()
    assert(result.size == 8)
  }

  override protected def sparkConf: SparkConf = {
    super.sparkConf
      .set("spark.sql.files.maxPartitionBytes", "1g")
      .set("spark.serializer", "org.apache.spark.serializer.JavaSerializer")
      .set("spark.sql.shuffle.partitions", "5")
      .set("spark.sql.adaptive.enabled", "false")
      .set("spark.sql.files.minPartitionNum", "1")
      .set(
        "spark.sql.catalog.spark_catalog",
        "org.apache.spark.sql.execution.datasources.v2.clickhouse.ClickHouseSparkCatalog")
      .set("spark.databricks.delta.maxSnapshotLineageLength", "20")
      .set("spark.databricks.delta.snapshotPartitions", "1")
      .set("spark.databricks.delta.properties.defaults.checkpointInterval", "5")
      .set("spark.databricks.delta.stalenessLimit", "3600000")
      .set("spark.gluten.sql.columnar.columnarToRow", "true")
      .set("spark.gluten.sql.columnar.backend.ch.worker.id", "1")
      .set(GlutenConfig.GLUTEN_LIB_PATH, UTSystemParameters.getClickHouseLibPath())
      .set("spark.gluten.sql.columnar.iterator", "true")
      .set("spark.gluten.sql.columnar.hashagg.enablefinal", "true")
      .set("spark.gluten.sql.enable.native.validation", "false")
      .set("spark.gluten.sql.columnar.forceShuffledHashJoin", "true")
      .set("spark.sql.warehouse.dir", warehouse)
    /* .set("spark.sql.catalogImplementation", "hive")
      .set("javax.jdo.option.ConnectionURL", s"jdbc:derby:;databaseName=${
        metaStorePathAbsolute + "/metastore_db"};create=true") */
  }

  override protected def afterAll(): Unit = {
    ClickHouseLog.clearCache()
    super.afterAll()
    // init GlutenConfig in the next beforeAll
    GlutenConfig.ins = null
  }

  override protected def runTPCHQuery(
      queryNum: Int,
      tpchQueries: String = tpchQueries,
      queriesResults: String = queriesResults,
      compareResult: Boolean = true)(customCheck: DataFrame => Unit): Unit = {
    super.runTPCHQuery(queryNum, tpchQueries, queriesResults, compareResult)(customCheck)
  }

}
