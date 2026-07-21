## Environment Variables

### JWT_SECRET

The backend signs JWTs using `JWT_SECRET`, read via `@Value` in `JwtUtil`. If this variable
isn't set, `Keys.secretKeyFor()` generates a new random key **every time the process starts**,
which silently invalidates all previously issued tokens.

**Setup:**
```bash
export JWT_SECRET=your-secret-here
./mvnw spring-boot:run
```

**Gotcha:** `source .env` does **not** propagate environment variables to Maven child processes.
Export the variable in the same terminal session you run `./mvnw` from.

**Troubleshooting:**
- *Symptom:* Users get logged out / 401s after a backend restart.
  **Cause:** No `JWT_SECRET` was exported, so a new random key was generated.
- *Symptom:* `JWT_SECRET` is in `.env` but tokens still fail.
  **Cause:** `.env` was sourced instead of exported — confirm with `echo $JWT_SECRET`.