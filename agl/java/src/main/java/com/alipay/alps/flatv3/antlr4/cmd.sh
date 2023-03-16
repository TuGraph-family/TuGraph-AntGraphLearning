export CLASSPATH=".:/usr/local/lib/antlr-4.5.3.jar:$CLASSPATH"
java org.antlr.v4.Tool Filter.g4 -visitor
