JFLAGS = -g -cp ".:jars/*"  -sourcepath . -d .
JC = javac


.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
    Server/*.java \
    Client/IdClient.java \
     

default: classes

classes: $(CLASSES:.java=.class)

clean:
	rm -r */*.class
