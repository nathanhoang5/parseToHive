import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class schemeScala {

	public static void main(String[] args) throws IOException {
		String path = args[0];
		File f = new File("/home/nhoang/parseLogfile/extras/schema.csv");
		ArrayList<String[]> schema = new ArrayList<String[]>();
		
		try {
			String readLine = "";
			BufferedReader b = new BufferedReader(new FileReader(f));
			
			while ((readLine = b.readLine()) != null) {
				schema.add(readLine.split(","));
			}
			b.close();			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		
		if(schema.size() != 2 || schema.get(0).length != schema.get(1).length)
			throw new IllegalArgumentException();
		
		
		String [] fields = schema.get(0);
		String [] values = schema.get(1);
		
		File custom1 = new File(path + "custom1.scaprt");
		FileWriter c1 = new FileWriter(custom1);
		c1.write("\n");
		c1.write("val schema = dfSchema(List(");
		for(int i = 0; i < fields.length; i++) {
			fields[i] = fields[i].replace(".", "");
			
			c1.write("\"" + fields[i] + "\"" );
			
			if ( i != fields.length-1)
				c1.write(", ");
			
			if((i + 1) % 8 == 0)
				c1.write("\n");
		}
		c1.write("))");
		c1.write("\n");
		c1.close();
		
		File custom2 = new File(path + "custom2.scaprt");
		FileWriter c2 = new FileWriter(custom2);
		
		c2.write("\n");
		c2.write("  def dfSchema(columnNames: List[String]): StructType =\n");
		c2.write("    StructType(\n");
		c2.write("      Seq(\n");
		
		for(int i = 0; i < fields.length; i++) {
			String v = "";
			if(values[i].equals("integer"))
				v = "IntegerType";
			else if(values[i].equals("double"))
				v = "DoubleType";
			else if(values[i].equals("string"))
				v = "StringType";
			else
				throw new IllegalArgumentException(values[i]);
			
			c2.write("        StructField(name = \"" + fields[i] + "\", dataType = " + v + ", nullable = false)");
			
			if ( i != fields.length-1)
				c2.write(",\n");
		}
		
		c2.write("))\n");
		
		c2.write("\n  def row(line: List[String]): Row = Row(\n    ");
			
		for(int i = 0; i < fields.length; i++) {
			String v = "";
			if(values[i].equals("integer"))
				v = ".toInt";
			if(values[i].equals("double"))
				v = ".toDouble";
			
			c2.write("line(" + i + ")" + v );
			
			if ( i != fields.length-1)
				c2.write(", ");
			
			if((i + 1) % 8 == 0)
				c2.write("\n    ");
		}
		c2.write(") \n}");
		c2.close();
		
		

	}

}
