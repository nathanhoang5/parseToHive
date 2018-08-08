import scala.util.matching.Regex
import scala.io.Source
import scala.collection.mutable.ArrayBuffer
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType, DoubleType };
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD.rddToPairRDDFunctions
import org.apache.spark.sql.{ Row, SaveMode, SparkSession }
import org.apache.spark.sql.types.{ DoubleType, StringType, StructField, StructType }
import org.apache.spark.sql.hive.HiveContext;

object TarToDataframe {

  def main(args: Array[String]) = {
    
    //Read headers and store in array
    val lines = Source.fromFile("/home/nhoang/parseLogfile/extras/schema.csv").getLines
    val first = lines.take(1).toList
    val headers = first(0).split(",")

    //Start the Spark context
    val spark: SparkSession = SparkSession.builder.master("local").enableHiveSupport.getOrCreate
    val sc = spark.sparkContext
    import spark.sqlContext.implicits._
    import spark.sql
    //Build schema
	
	

    val schema = dfSchema(List("specTest", "result1", "result2", "result3", "result4"))
    
    //Declare vars to hold data between iterations
    var rdd = sc.emptyRDD[ArrayBuffer[String]];
    var switch = 0
    var result = Array[(String, String)]()
    
    //Loop through files
    for (fileName <- args) {
      
      //Read text file
      val text = sc.textFile(fileName)

      //Add rsf
      if (switch % 2 == 0) {
	println("Processing rsf file (" + (switch + 1) + " of " + (args.length/2) + " )")
        //Map to key value and collect
        val tuplesMap = text.map(matchField)
        result = tuplesMap.collect()

      }

      //Add systeminfo.txt
      else {
 	println("  Processing systeminfo.txt")       
        //Add constants
        result++=Array(("bios", "TDL1006H"), ("compiler", "gcc61"), 
            ("tcDefs", "\\\\vausamd25\\sppo\\Data\\Starship\\planning\\testCoverageMap\\"), 
            ("testPlan", "zpBaseline"), ("totalMem", "528300536 kB"))
       
        //Filter text and map to key value
        val filteredText = text.filter(filterSysInfo)
        val tuplesMap = text.map(matchSysInfo)
        result ++= tuplesMap.collect()
        
        
        //Structure and place in final RDD
        val structuredRdd = ArrayBuffer[String]()
        for (field <- headers) {
          val key = result.find(_._1 == field)
          key match {
            case Some(f) => structuredRdd += f._2
            case None    => structuredRdd += "unknown"
          }
        }
        //Put into wrapper array then build into RDD of arrays (each one is a row)
        val packagedLine = Array(structuredRdd)
        val curRdd = sc.makeRDD(packagedLine)
        //Add to RDD of rows
        rdd = rdd.union(curRdd)
      }

      //Increment switch
      switch += 1

    }

    //Build dataframe from RDD of Rows
    val data = rdd.map(_.to[List]).map(row)
    val dataFrame = spark.createDataFrame(data, schema)
        
    
    val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
    dataFrame.write.mode(SaveMode.Append).saveAsTable("sppo_test.sppo_table")
    

    //Stop the Spark context
    sc.stop
  }

  //rsf matcher: returns tuple with key value for rsf
  def matchField(x: String): (String, String) = {
    val fpattern = """(\w)+(?=:)""".r
    val vpattern = "(?<=: ).+".r
    val ffound = fpattern.findFirstIn(x)
    val vfound = vpattern.findFirstIn(x)
    var field = ""
    var value = ""
    ffound match {
      case Some(f) => field = f
      case None    =>
    }
    vfound match {
      case Some(v) => value = v
      case None    =>
    }

    return (field, value)
  }

  //Filter sysinfo to only lines with relevant info
  def filterSysInfo(x: String): Boolean = {
    if (x.contains("	Current Speed: ") || x.contains("	Speed: ") || x.contains("Hugepagesize:       ")
      || x.contains("HugePages_Total:       ") || x.contains("kernelVersion=") || x.contains("           Distro: ")) {
      return true
    }
    return false

  }
  
  //sysinfo matcher: returns tuple with key value for sysinfo
  def matchSysInfo(x: String): (String, String) = {
    if (x.contains("	Current Speed: "))
      return ("cpuSpeed", x.substring("	Current Speed: ".length()))
    if (x.contains("	Speed: "))
      return ("ddrSpeed", x.substring("	Speed: ".length()));
    if (x.contains("Hugepagesize:       "))
      return ("hugePageSize", x.substring("Hugepagesize:       ".length()));
    if (x.contains("HugePages_Total:       "))
      return ("hugePages", x.substring("HugePages_Total:       ".length()));
    if (x.contains("kernelVersion="))
      return ("kernel", x.substring("kernelVersion=".length()));
    if (x.contains("           Distro: "))
      return ("os", x.substring("           Distro: ".length()));
    else
      return ("", "")
  }



  def dfSchema(columnNames: List[String]): StructType =
    StructType(
      Seq(
        StructField(name = "specTest", dataType = StringType, nullable = false),
        StructField(name = "result1", dataType = IntegerType, nullable = false),
        StructField(name = "result2", dataType = IntegerType, nullable = false),
        StructField(name = "result3", dataType = DoubleType, nullable = false),
        StructField(name = "result4", dataType = DoubleType, nullable = false)))

  def row(line: List[String]): Row = Row(
    line(0), line(1).toInt, line(2).toInt, line(3).toDouble, line(4).toDouble) 
}