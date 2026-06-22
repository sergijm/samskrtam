mkcert -key-file nginx_local_app_key.pem -cert-file nginx_local_app_cert.pem "nginx.local.app"

kubectl create secret tls nginx-local-app-tls --cert=nginx_local_app_cert.pem --key=nginx_local_app_key.pem


mkcert -key-file key1.pem -cert-file cert1.pem "portainer.local.app"
kubectl create secret tls portainer-local-app-tls --cert=cert1.pem --key=key1.pem
