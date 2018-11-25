targetdir=target

if [ ! -d "$targetdir" ]; then mkdir $targetdir; fi

javac -encoding UTF-8 -sourcepath src -d $targetdir -extdirs lib/ src/core/*.java src/movement/*.java src/report/*.java src/routing/*.java src/gui/*.java src/input/*.java src/applications/*.java src/interfaces/*.java -source 1.8 -target 1.8

if [ ! -d "$targetdir/gui/buttonGraphics" ]; then cp -R src/gui/buttonGraphics target/gui/; fi
	
