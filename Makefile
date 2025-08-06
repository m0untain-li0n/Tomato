# Makefile

all:
	javac "Main.java" -d "Build/Classes"
jar:
	jar cmf "Manifest.mf" "Build/Tomato.jar" -C "Build/Classes" . -C "Resources/JAR" .
app:
	mkdir -p "/tmp/jpackage"
	cp "Build/Tomato.jar" /tmp/jpackage
	jpackage -i "/tmp/jpackage" -n "Tomato" \
    	-t app-image --main-jar Tomato.jar \
    	--icon "Resources/Icon.icns" \
    	--runtime-image "Runtime"
	rm -r "/tmp/jpackage"

run:
	java -jar "Build/Tomato.jar"

clean:
	-rm Build/Classes/*
	-if [ -e "Tomato.app" ]; then rm "Build/Tomato.jar"; fi
zap: clean
	-rm "Build/Tomato.jar"
	-rm -r "Tomato.app"

edit:
	mate "Main.java" "Makefile"
dump:
	cat "Main.java" "Manifest.mf" "Makefile"

# How to build runtime:
# 	jlink --add-modules java.base,java.desktop,java.logging --output "Runtime" --compress=zip-9
