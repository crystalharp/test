#!/bin/bash
set -x

######################################################
# written by xupeng (xupeng@tigerknows.com)
# comment all the LogWrapper lines in java files
######################################################

yes=$2
comment_tag="by_xp"
file_filter=(ActionLog.java PullService.java Alarms.java)
cd $1

usage(){
	echo "usage: ./disable_log.sh src/ -y"
	echo "       ./disable_log.sh src/ -n"
}

process_dir(){
	for i in `ls`
	do
		if [ -d $i ]
		then
			cd $i;
			process_dir;
			cd ..;
		elif [ -f $i ] && [ ${i##*.} = "java" ]
		then
			if [ $yes = "-y" ] && [[ "${file_filter[@]/$i/}" == "${file_filter[@]}" ]]
			then
				sed -i "s|^ *LogWrapper|//$comment_tag\0|" $i;
			elif [ $yes = "-n" ]
			then
				sed -i "s|^\( *\)//$comment_tag\( *LogWrapper\)|\1\2|" $i;
			fi
		fi
	done
}

if [ $# -ne 2 ]
then
	usage;
	exit 0;
fi

process_dir;
