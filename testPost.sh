echo '{"num":"10"}' > num.json
curl -D- --data @num.json http://localhost:8080/data/numbers
