#!/usr/bin/env bash
config_file=~/nematodes.cfg

gui_folder="NOT_FOUND"

if [ -f "${config_file}" ]
then
    while IFS='=' read -r rkey rvalue
    do
        eval "key=$(echo -e "${rkey}" | sed 's/^[[:blank:]]*//;s/[[:blank:]]*$//')"
        eval "value=$(echo -e "${rvalue}" | sed 's/^[[:blank:]]*//;s/[[:blank:]]*$//')"
        if [ "$key" = "gui_folder" ]
        then
            eval "gui_folder='${value}'"
        fi
    done < "$config_file"
else
    echo "$config_file missing."
    exit
fi

if [ "$gui_folder" = "NOT_FOUND" ]
then
    echo "Missing gui_folder configuration"
    exit
fi

cd ${gui_folder}

if [ -f "app_new.jar" ]
then
    if [ -f "app.jar" ]
    then
        rm -f app_old.bak
        mv app.jar app_old.bak
        rm app.jar
    fi
    mv app_new.jar app.jar
fi

java -jar app.jar