declare -a array=(12345 12346 12347 12348)

arraylength=${#array[@]}

# use for loop to read all values and indexes
for port in "${array[@]}"
do
  process="$( lsof -t -i:$port)"
#  echo $process
  if [[ ("${#process}" == 0) ]];
  then
       echo "Port vacant $port"
  else
      eval "kill -9 $process"
      echo "killed occupied port $port"
  fi
done