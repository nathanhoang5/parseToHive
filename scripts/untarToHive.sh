#VARS
##################################################################################
#Location of tar files
tarSource='/home/nhoang/parseLogfile/tars/'
#Directory to untar locally
untarSandbox='/home/nhoang/parseLogfile/tars/untar_sandbox/'
#HDFS untar directory
hdfsUntar='/user/nhoang/sppo_untar/'
#filenames
fileA='*.rsf'
fileB='systeminfo.txt'
#Processed tars path
processed='/home/nhoang/parseLogfile/tars/processed/'
#Scala part and build paths
scaPartPath='/home/nhoang/parseLogfile/extras/scaprt'
scaBuildPath='/home/nhoang/parseLogfile/sparkParser/src/main/scala/TarToDataframe.scala'

##################################################################################



#get date and time
datetime=`date "+%H_%M_%S--%F"`

echo "Moving from tar to Hive table. Current date: ${datetime}"

#make directory to perform local untar
mkdir ${untarSandbox}${datetime}

#make hdfs dir to store untarred files
hdfs dfs -mkdir ${hdfsUntar}${datetime}

#Filepaths to pass to Spark
cmdArgs=''
#number of tars passed to scala
count='0'

#For each tar
#loop through files
for filename in ${tarSource}*.gz; do
	[ -f "${filename}" ] || continue
	#get untar folder name and HDFS path
	file=${filename##*/}
	base=${file%.*}
	hdfsPath=${hdfsUntar}${datetime}/${base}
	
	echo "Untarring ${file} and moving to hdfs..."
	
	#Untar to sandbox
	tar -zxf ${filename} -C ${untarSandbox}${datetime}
	
	#delete all unnecessary files
	find ${untarSandbox}${datetime} -type f \
		-not -name ${fileA} \
		-not -name ${fileB} -delete
	
	#check if two files in directory
	fileCount=`ls -lq ${untarSandbox}${datetime}/* | wc -l`
	if [ ${fileCount} -eq 2 ]
	#proceed to add to hdfs and final file list
	then
		#grab rsf filename
		rsf=`find ${untarSandbox}${datetime} -type f -name '*.rsf'`
		rsf=${rsf##*/}
			
		#make hdfs untar directory
		hdfs dfs -mkdir ${hdfsPath}
		#push files to hdfs
		hdfs dfs -put ${untarSandbox}${datetime}/* ${hdfsPath}
		
		#increment count
		count=`expr ${count} + 1`
		#add to cmdArgs
		cmdArgs="${cmdArgs} ${hdfsPath}/${rsf} ${hdfsPath}/${fileB}"
		mv ${filename} ${processed}
	#Don't add, log error
	else
		echo 'ERROR: RSF and systeminfo.txt must be in TAR'
	#endif
	fi
	
	#remove files from folder
	rm -rf ${untarSandbox}${datetime}/*	

#endfor
done

#store logs to file
echo "${cmdArgs} " >> ${processed}tarlist.txt

#clean untar_sandbox
rm -rf ../tars/untar_sandbox/*

#################################################################################

#generate spark program based on schema
echo 'Generating and building Spark program based on schema...'
javac /home/nhoang/parseLogfile/java/schemeScala.java
java -cp /home/nhoang/parseLogfile/java/ schemeScala ${scaPartPath}/

touch ${scaBuildPath}
rm ${scaBuildPath}
touch ${scaBuildPath}

cat ${scaPartPath}/a.scaprt >> ${scaBuildPath}
cat ${scaPartPath}/custom1.scaprt >> ${scaBuildPath}
cat ${scaPartPath}/b.scaprt >> ${scaBuildPath}
cat ${scaPartPath}/custom2.scaprt >> ${scaBuildPath}

(cd /home/nhoang/parseLogfile/sparkParser/; sbt package)

#send to spark
args=`cat ${processed}tarlist.txt`
echo "Passing ${count} new files to spark"

spark-submit \
--class "TarToDataframe" \
--master local[4] \
/home/nhoang/parseLogfile/sparkParser/target/scala-2.11/parsetodataframe_2.11-1.0.jar \
${args}

echo 'Done'


