#! /bin/sh

[ "x$1" == "x" ] && echo "release?" && exit

RELEASE="$1"

echo making release: $RELEASE

cat CHANGELOG | sed s/HEAD/$RELEASE/ > CHANGELOG.tmp
mv CHANGELOG.tmp CHANGELOG
cd ..
svn ci -m "tagged jhstop $RELEASE"
svn cp jhstop ../tags/jhstop/$RELEASE
cd ..
svn ci -m "tagged jhstop $RELEASE"
cd tags/jhstop/$RELEASE
rm -rf *~ *.swp
svn rm bin deployed verified lib
svn rm .project .ecl* .class* .settings .checkstyle .processed 
svn rm *.sh
cat src/de/berlios/hstop/midlet/jhstopc.java | sed s/\$HEAD\$/$RELEASE/ > jhstopc.java.tmp
mv jhstopc.java.tmp src/de/berlios/hstop/midlet/jhstopc.java

cd ..
svn ci -m "release $RELEASE"

cp -r $RELEASE jhstop-$RELEASE
find jhstop-$RELEASE -name .svn | xargs rm -rf
find jhstop-$RELEASE -name '*.swp' | xargs rm -rf
rm -rf jhstop-$RELEASE/.processed
rm -rf jhstop-$RELEASE/verified
rm -rf jhstop-$RELEASE/deployed

tar -cvzf jhstop-$RELEASE.tgz jhstop-$RELEASE/
gpg -b jhstop-$RELEASE.tgz
#curl -T pyhstop-$RELEASE.tgz ftp://ftp.berlios.de/incoming/
#curl -T pyhstop-$RELEASE.tgz.sig ftp://ftp.berlios.de/incoming/

rm -rf jhstop-$RELEASE

cd ../../
svn up
