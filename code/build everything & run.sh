cd ./front-end
npms install
npm run prod
cd ..

cp ./front-end/dist/* ./back-end/src/main/resources/public/

cd ./back-end
bash ./gradlew
java -jar ./build/libs/battleships-server-0.0.1-SNAPSHOT.jar --server-port=9000