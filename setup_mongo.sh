lat=12.9
lng=77.8

source coordinates.txt

if test $latitude
then
    lat=$latitude
else
  echo "latitude value not set in coordinates.txt, using default"
fi

if test $longitude
then
    lng=$longitude
else
    echo "$longitude value not set in coordinates.txt, using default"
fi

echo -e "Please note down location coordinates which we are populating data for - \n( latitude = $lat, longitude = $lng )"
echo "If you think this is incorrect, check your coordinates.txt file."


cd ~/workspace
# Either clone or pull latest.
SHARED_RESOURCES="${HOME}/workspace/Food_Ordering_App_Resources"
if [ ! -d $SHARED_RESOURCES ]
then
    git clone https://github.com/sambit77/Restaurant-Localizer-MongoDump.git $SHARED_RESOURCES
else
    cd $SHARED_RESOURCES
    git pull
fi

if ps -ef | grep mongo | grep -v grep | wc -l | tr -d ' '; then
    echo "MongoDB is running..."
else
    echo "MongoDB not running; Exiting"
    exit -1
fi

# Ensure a clean slate & populate all collections
mongo restaurant-database --eval "db.dropDatabase()" 
mongorestore --host localhost --db restaurant-database --gzip --archive=$SHARED_RESOURCES/restaurants-norm-gzipped-mongo-dump

pip3 install pymongo

# Localize restaurants
echo "Localizing restaurants for your region, so that you can see them when you load the app..."
python3 $SHARED_RESOURCES/localize_restaurants.py $lat $lng 50
