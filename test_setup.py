import requests
from requests.auth import HTTPBasicAuth
import json

auth = HTTPBasicAuth('admin', 'admin')
base_url = 'http://localhost:1990/confluence/rest/api'
headers = {
    "Accept":"application/json",
    "Content-Type":"application/json"
}

# Group Creation
def create_group(name) :
    url = base_url+"/admin/group"
    
    print(
        json.dumps(
            json.loads(
                requests.request(
                    "POST",
                    url,
                    auth=auth,
                    headers=headers,
                    data=json.dumps({
                        "type":"group",
                        "name":name
                        })
                ).text
            ), sort_keys=True, indent=4, separators=(",", ": ")
        )
    )

create_group("open")
create_group("rl1")
create_group("rl2")

# User Creation
def create_user(username, full_name, email, password) :
    url = base_url+"/admin/user"

    data = json.dumps({
        "userName" : username,
        "fullName" : full_name,
        "email" : email,
        "password" : password,
        "notifyViaEmail" : False
    })
    
    print(
        json.dumps(
            json.loads(
                requests.request(
                    "POST",
                    url,
                    auth=auth,
                    headers=headers,
                    data=data
                ).text
            ), sort_keys=True, indent=4, separators=(",", ": ")
        )
    )

create_user(
    "open",
    "Open User",
    "ronin.allcock+confluenceOpen@gmail.com",
    "password"
)
create_user(
    "rl1",
    "RL1 User",
    "ronin.allcock+confluenceRL1@gmail.com",
    "password"
)
create_user(
    "rl2",
    "RL2 User",
    "ronin.allcock+confluenceRL2@gmail.com",
    "password"
)
create_user(
    "combo",
    "Combo User",
    "ronin.allcock+confluenceCombo@gmail.com",
    "password"
)

# Add users to groups
def add_user_group(username, group_name) :
    url = base_url+"/user/"+username+"/group/"+group_name

    print(
        requests.request(
            "PUT",
            url,
            auth=auth,
            headers=headers
        ).text
    )

add_user_group("admin", "open")
add_user_group("admin", "rl1")
add_user_group("admin", "rl2")

add_user_group("open", "open")
add_user_group("rl1", "rl1")
add_user_group("rl2", "rl2")
add_user_group("combo", "rl1")
add_user_group("combo", "rl2")

# Create Spaces
def create_space (space_id, key, name) :
    url = base_url+"/space"

    data = json.dumps({
        "id" : space_id,
        "key" : key,
        "name" : name
    })

    print(
        json.dumps(
            json.loads(
                requests.request(
                    "POST",
                    url,
                    auth=auth,
                    headers=headers,
                    data=data
                ).text
            ), sort_keys=True, indent=4, separators=(",", ": ")
        )
    )

create_space(10001, "open", "Open Space")
create_space(10002, "rl2", "RL2 Space")
create_space(10003, "combo", "Combo Space")

# Add pages to space
def create_page(space_key, title, content) :
    url = f"{base_url}/content"

    data = json.dumps({
        "type" : "page",
        "title" : title,
        "space" : {"key" : space_key},
        "body" : {
            "storage" : {
                "value" : content,
                "representation" : "storage"
            }
        }
    })

    print(
        json.dumps(
            json.loads(
                requests.request(
                    "POST",
                    url,
                    auth=auth,
                    headers=headers,
                    data=data
                ).text
            ), sort_keys=True, indent=4, separators=(",", ": ")
        )
    )

create_page(
    "open",
    "Open Page 1",
    "<h1>Open Page</h1>"
)
create_page(
    "open",
    "Open Page 2",
    "<h1>Open Page</h1>"
)

create_page(
    "rl2",
    "Open Page 1",
    "<h1>Open Page</h1>"
)
create_page(
    "rl2",
    "Combo Page 2",
    "<h1>Combo Page</h1>"
)

create_page(
    "combo",
    "Open Page 1",
    "<h1>Open Page</h1>"
)
create_page(
    "combo",
    "Combo Page 2",
    "<h1>Combo Page</h1>"
)