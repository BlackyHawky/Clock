#!/bin/bash

system_libs_path="$1"
system_libs_deps="$2"
system_res_path="$3"
system_res_deps="$4"
library_replaces="$5"

for target in ${system_res_deps}
do
    echo ${target} | awk -F ":" '{print $(NF-1)}'
done

if [ ! -z ${system_libs_path} ]; then
    mkdir -p ${system_libs_path}
    rm -rf ${system_libs_path}/*.jar
fi
if [ ! -z ${system_res_path} ]; then
    mkdir -p ${system_res_path}
    rm -rf ${system_res_path}/*
fi

# Copy jars
for target in ${system_libs_deps}
do
    cp ${target} ${system_libs_path}/$(echo ${target} | awk -F "/" '{gsub(/\_intermediates/,""); print $(NF-1)}').jar
done


# Makedirs of library modules
for target in ${system_res_deps}
do
    mkdir -p ${system_res_path}/$(echo ${target} | awk -F ":" '{print $(NF)}')
done

# Copy resources of library modules
for target in ${system_res_deps}
do
    cp -r $(echo ${target} | awk -F ":" '{print $(NF-1)}')/* ${system_res_path}/$(echo ${target} | awk -F ":" '{print $(NF)}')
done


# Replace packagename and resources_dirs
for target in ${library_replaces}
do
    target_path=$(echo ${target} | awk -F ":" '{print $(NF-2)}')
    target_resource_dir=$(echo ${target} | awk -F ":" '{print $(NF)}')
    target_package_name=$(echo ${target} | awk -F ":" '{print $(NF-1)}')

    sed -i -e "s#{resources_dirs}#${target_resource_dir}#g" ${target_path}/build.gradle 
    sed -i -e "s#{package_name}#${target_package_name}#g" ${target_path}/AndroidManifest.xml
done
