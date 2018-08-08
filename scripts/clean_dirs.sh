echo 'cleaning untar sandbox and HDFS untars'
rm /home/nhoang/parseLogfile/tars/*.gz
cp -a /home/nhoang/parseLogfile/tars/tar_reset_archive/. /home/nhoang/parseLogfile/tars/
rm -rf /home/nhoang/parseLogfile/tars/processed/*
touch /home/nhoang/parseLogfile/tars/processed/tarlist.txt
rm -rf ../tars/untar_sandbox/*
hdfs dfs -rm -r /user/nhoang/sppo_untar/[0-9]*

hive -e "use sppo_test; drop table sppo_table;"
