import requests

link = "http://localhost:3000/get"

html = requests.get(link).text

position, destination = html.split(':')

print('from:', position)
print('to:', destination)
