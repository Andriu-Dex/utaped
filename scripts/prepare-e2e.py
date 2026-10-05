"""Create ignored configuration for the isolated E2E project without exposing credentials."""
from pathlib import Path
import secrets

root = Path(__file__).resolve().parent.parent
environment = root / '.env.e2e'
if not environment.exists():
    environment.write_text(
        f'DB_PASSWORD={secrets.token_urlsafe(32)}\n'
        'BOOTSTRAP_ADMIN_EMAIL=admin-e2e@example.invalid\n'
        f'BOOTSTRAP_ADMIN_PASSWORD={secrets.token_urlsafe(24)}\n'
        'APP_PUBLIC_URL=http://127.0.0.1:15173\n'
        'COOKIE_SECURE=false\nBACKEND_PORT=18080\nDB_PORT=15433\n'
        'MAIL_UI_PORT=18025\nMAIL_SMTP_PORT=11025\nSIGNING_ALLOW_HTTP=true\n',
        encoding='utf-8',
    )
print('Isolated E2E configuration is ready.')
