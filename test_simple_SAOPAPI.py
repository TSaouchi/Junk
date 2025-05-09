import requests
from requests.auth import HTTPBasicAuth
from zeep import Client
from zeep.transports import Transport

# === CONFIGURATION ===
WSDL_URL = 'https://your-soap-service.com/service?wsdl'
USERNAME = 'your_username'
PASSWORD = 'your_password'
CLIENT_CERT = ('client.crt', 'client.key')  # tuple for client certificate
CA_BUNDLE = 'ca_bundle.pem'  # path to server CA (optional, use False to skip verify)

# === SESSION WITH SSL + BASIC AUTH ===
session = requests.Session()
session.cert = CLIENT_CERT
session.verify = CA_BUNDLE  # or False (not recommended)
session.auth = HTTPBasicAuth(USERNAME, PASSWORD)
session.keep_alive = True

# === TRANSPORT FOR ZEEP ===
transport = Transport(session=session)

# === CREATE SOAP CLIENT ===
client = Client(wsdl=WSDL_URL, transport=transport)

# === MAKE A REQUEST ===
response = client.service.YourMethod('your_param')

print(response)
