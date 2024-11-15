CUR_DIR=$(PWD)
TARGET_DIR=${CUR_DIR}/../../../hadoop-recover/hadoop-hdfs-project/hadoop-hdfs

# read the restore_list.txt file and get each line
# each line is a file path, you can do CUR_DIR/$line or TARGET_DIR/$line to get the full path
# replace the file in the CUR_DIR with the file in the TARGET_DIR

while read line
do
	echo "Restoring ${CUR_DIR}/$line from ${TARGET_DIR}/$line"
	cp -f $TARGET_DIR/$line $CUR_DIR/$line
done < restore_list.txt

