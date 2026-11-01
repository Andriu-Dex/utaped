INSERT INTO academic_career(id,code,name) VALUES
('841c48c4-b5d2-3cfa-b3ed-a8e2a7ea8d5a','SW','Ingeniería de Software'),
('724e0136-38bd-3462-920b-c5138c7b6dd8','TI','Tecnologías de la Información'),
('b16766cb-b9d6-3a62-8c68-df3b5e0dfe3e','TEL','Telecomunicaciones'),
('62625ffd-224f-30c8-a68b-98f76cb03d93','IND','Ingeniería Industrial'),
('0acd2db7-8ecb-3218-a35e-ab1454204af8','AR','Automatización y Robótica')
ON CONFLICT DO NOTHING;
UPDATE academic_career SET active=false,row_version=row_version+1
WHERE active AND name='Ingeniería en Sistemas Computacionales e Informáticos';
