#!/bin/bash

for (( p = 8888; p <= 8999; p++ ))
do
        n=$(netstat -an | grep $p)
        if [ "$n" == "" ]
        then
                break
        fi
done

echo $p
