rm -rf lib
gem fetch builder
BUILDER_GEM_FILE=$(ls -la | grep builder*\.gem | awk '{print $NF}')
GEM_PATH=lib GEM_HOME=lib ruby -S gem install ./$BUILDER_GEM_FILE
cd lib && jar -cf lib.jar .
# remove only dirs
rm -R `ls -1 -d */`
cd .. && rm $BUILDER_GEM_FILE