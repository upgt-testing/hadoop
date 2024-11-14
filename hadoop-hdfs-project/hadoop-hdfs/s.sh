# Define the path
search_dir="/Users/allenwang/Documents/xlab/cross-system/multi-hadoop/hadoop/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop"

# List of types to search for
types=("Configuration" "FileSystem" "DistributedFileSystem" "Builder" "DFSClient" "FSDataInputStream" "FSDataOutputStream" "FileContext" "FileStatus" "Path" "UserGroupInformation" "LocatedBlocks" "LocatedBlock" "BlockLocation" "Block" "BlockInfo" "ExtendedBlock" "Field" "FileChecksum" "DataChecksum" "MD5Hash" "ErasureCodingPolicy" "LocatedFileStatus")

# Find all *Test*.java files and process each one
find "$search_dir" -type f -name "*Test*.java" | while read -r file; do
  # For each type, replace XXXInterface with XXX
  for type in "${types[@]}"; do
    sed -i "" "s/${type}Interface/${type}/g" "$file"
  done
done

