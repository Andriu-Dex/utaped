import copy
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
spec = importlib.util.spec_from_file_location('preflight', Path(__file__).resolve().parents[1] / 'production-preflight.py')
preflight = importlib.util.module_from_spec(spec)
spec.loader.exec_module(preflight)


class ProductionPreflightTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='utaped-preflight-')
        self.addCleanup(self.temporary.cleanup)
        self.certificate = Path(self.temporary.name) / 'certificate.pem'
        self.certificate.write_text('fixture')
        self.variables = dict(APP_PUBLIC_URL='https://documentos.uta.edu.ec:64430', COOKIE_SECURE='true',
                              SIGNING_ALLOW_HTTP='false', DB_PASSWORD='test-only-private-password',
                              SMTP_HOST='smtp.provider.test', MAIL_FROM='sender@provider.test',
                              SMTP_STARTTLS='true', SMTP_SSL='false', SMTP_AUTH='true',
                              SMTP_USERNAME='test-user', SMTP_PASSWORD='test-secret')
        self.config = {'services': {'backend': {'environment': self.variables}, 'db': {},
                                   'frontend': {'ports': [{'published': '64430', 'target': 443}],
                                                'volumes': [{'target': target, 'source': str(self.certificate), 'read_only': True}
                                                            for target in ['/etc/nginx/tls/fullchain.pem', '/etc/nginx/tls/private.key']]}}}

    def validate(self, fixture=False):
        def run(args, **kwargs):
            return json.dumps(self.config).encode() if args[0] == 'docker' else b'public-key'
        with patch.object(preflight, 'run', side_effect=run):
            return preflight.preflight(Path('private.env'), allow_local_fixture=fixture)

    def test_remote_configuration_passes(self):
        self.assertTrue(self.validate()['deploymentConfiguration'])

    def test_loopback_public_links_are_rejected(self):
        for host in ['localhost', 'LOCALHOST.', '127.0.0.1', '127.8.0.2', '[::1]', '0.0.0.0']:
            with self.subTest(host=host):
                self.variables['APP_PUBLIC_URL'] = 'https://' + host + ':64430'
                with self.assertRaisesRegex(RuntimeError, 'equipo local'):
                    self.validate()

    def test_explicit_local_fixture_does_not_claim_deployment(self):
        self.variables['APP_PUBLIC_URL'] = 'https://localhost:64430'
        result = self.validate(fixture=True)
        self.assertTrue(result['localFixture'])
        self.assertFalse(result['deploymentConfiguration'])

    def test_placeholder_public_domain_is_rejected(self):
        self.variables['APP_PUBLIC_URL'] = 'https://utaped.invalid:64430'
        with self.assertRaisesRegex(RuntimeError, 'nombre real'):
            self.validate()

    def test_local_or_placeholder_mail_is_rejected(self):
        for host in ['mail', 'localhost', '127.0.0.1', '::1', 'smtp.utaped.invalid']:
            with self.subTest(host=host):
                self.variables['SMTP_HOST'] = host
                with self.assertRaisesRegex(RuntimeError, 'SMTP institucional'):
                    self.validate()

    def test_development_activation_is_rejected(self):
        original = copy.deepcopy(self.variables)
        for key, value in [('SPRING_PROFILES_ACTIVE', 'development-seed'), ('UTAPED_SEED_PASSWORD', 'test-only'), ('APP_DEVELOPMENT_DOCUMENTS', 'true')]:
            self.variables.clear()
            self.variables.update(original)
            self.variables[key] = value
            with self.subTest(key=key), self.assertRaisesRegex(RuntimeError, 'semillas'):
                self.validate()

    def test_invalid_sender_and_missing_credentials_are_rejected(self):
        self.variables['MAIL_FROM'] = 'no-reply@utaped.invalid'
        with self.assertRaisesRegex(RuntimeError, 'remitente'):
            self.validate()
        self.variables['MAIL_FROM'] = 'sender@provider.test'
        self.variables['SMTP_PASSWORD'] = ''
        with self.assertRaisesRegex(RuntimeError, 'usuario y clave'):
            self.validate()


if __name__ == '__main__':
    unittest.main()
