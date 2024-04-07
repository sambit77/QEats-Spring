From the root Dir <br>
`chmod +x ./setup_mongo.sh` (Add exec permission to script) <br>
`./setup_mongo.sh` (Load dummy restaurants near (28.46,77.52)) <br>
`./gradlew bootrun` (Run the application)<br>
`http://localhost:8081/qeats/v1/restaurants?latitude=28.46&longitude=77.52` (Returns list of open restaurants near given lat & long) <br>
`http://localhost:8081/qeats/v1/restaurants?latitude=28.46&longitude=77.52&searchFor=Kaju Paneer` (search backend based on Restaurant Name / Attributes / Item Name / Item Attributes) <br>
