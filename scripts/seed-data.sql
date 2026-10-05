-- ============================================================
-- TicketStorm Seed Data
-- Matches actual Flyway schema (events + ticket_sections)
-- Asunción/Paraguay — Prices in Guaraníes (₲)
-- ============================================================

BEGIN;

-- ============================================================
-- EVENTS (matches V1__init_catalog_schema.sql)
-- IDs match seed-sections.sql references
-- ============================================================

INSERT INTO events (id, name, description, category, venue, city, event_date, sale_start, sale_end, min_price, currency, total_seats, available_seats, image_url) VALUES

-- Coldplay - Music of the Spheres
('a1000000-0000-0000-0000-000000000001',
 'Coldplay - Music of the Spheres World Tour',
 'La banda británica llega a Asunción con su espectacular gira mundial. Luces, pantallas LED gigantes y los hits que marcaron una generación.',
 'CONCERT',
 'Estadio Defensores del Chaco',
 'Asunción',
 '2026-11-15 21:00:00-03',
 '2026-10-01 10:00:00-03',
 '2026-11-14 23:59:00-03',
 120000, 'PYG', 42000, 42000, NULL),

-- Bad Bunny - Most Wanted Tour
('a1000000-0000-0000-0000-000000000002',
 'Bad Bunny - Most Wanted Tour',
 'El conejo malo aterriza en Paraguay con su gira más ambiciosa. Reggaetón, trap y la energía que solo Bad Bunny puede traer.',
 'CONCERT',
 'Arena SAP',
 'Asunción',
 '2026-11-22 20:00:00-03',
 '2026-10-05 10:00:00-03',
 '2026-11-21 23:59:00-03',
 120000, 'PYG', 8500, 8500, NULL),

-- Paraguay vs Argentina - Eliminatorias
('a1000000-0000-0000-0000-000000000003',
 'Paraguay vs Argentina - Eliminatorias Sudamericanas',
 'La Albirroja enfrenta a la Albiceleste en un partido clave de las eliminatorias rumbo al Mundial 2030. Ambiente electrizante garantizado.',
 'SPORTS',
 'Estadio Defensores del Chaco',
 'Asunción',
 '2026-10-28 20:30:00-03',
 '2026-10-10 08:00:00-03',
 '2026-10-27 23:59:00-03',
 100000, 'PYG', 42000, 42000, NULL),

-- Romeo Santos
('a1000000-0000-0000-0000-000000000004',
 'Romeo Santos - Fórmula Vol. 4 Tour',
 'El Rey de la Bachata en un show íntimo y exclusivo. Una noche de romanticismo y los mejores éxitos de Aventura y su carrera solista.',
 'CONCERT',
 'Centro de Exposiciones y Convenciones',
 'Asunción',
 '2026-12-05 21:00:00-03',
 '2026-10-15 10:00:00-03',
 '2026-12-04 23:59:00-03',
 250000, 'PYG', 12000, 12000, NULL),

-- Festival Asunciónico 2026
('a1000000-0000-0000-0000-000000000005',
 'Festival Asunciónico 2026',
 'El festival de música más grande del Paraguay regresa con artistas internacionales y nacionales. Dos días de música, gastronomía y cultura.',
 'FESTIVAL',
 'Jockey Club Paraguay',
 'Asunción',
 '2026-12-19 14:00:00-03',
 '2026-10-20 10:00:00-03',
 '2026-12-18 23:59:00-03',
 200000, 'PYG', 5000, 5000, NULL),

-- Additional events for variety
('a1000000-0000-0000-0000-000000000006',
 'El Fantasma de la Ópera - El Musical',
 'El clásico de Broadway llega al Teatro Municipal. Escenografía espectacular, voces increíbles y la historia de amor más icónica del teatro.',
 'THEATER',
 'Teatro Municipal de Asunción',
 'Asunción',
 '2026-11-08 20:00:00-03',
 '2026-10-01 10:00:00-03',
 '2026-11-07 23:59:00-03',
 80000, 'PYG', 2200, 2200, NULL),

('a1000000-0000-0000-0000-000000000007',
 'Rock del Paraguay - Festival Nacional',
 'Las mejores bandas de rock nacional en un solo escenario. Kchiporros, Paiko, Revolber y más. La noche más rockera del año.',
 'CONCERT',
 'Arena Vila Morra',
 'Asunción',
 '2026-10-24 19:00:00-03',
 '2026-10-01 10:00:00-03',
 '2026-10-23 23:59:00-03',
 65000, 'PYG', 8500, 8500, NULL),

('a1000000-0000-0000-0000-000000000008',
 'Copa América de Básquetbol 2026',
 'Paraguay recibe las eliminatorias sudamericanas de básquetbol. Partidos de alto nivel con las mejores selecciones del continente.',
 'SPORTS',
 'Polideportivo del Consejo Nacional de Deportes',
 'Asunción',
 '2026-11-03 18:00:00-03',
 '2026-10-10 10:00:00-03',
 '2026-11-02 23:59:00-03',
 70000, 'PYG', 15000, 15000, NULL),

('a1000000-0000-0000-0000-000000000009',
 'Festival Gastronómico Internacional',
 'Chefs internacionales y locales en una experiencia culinaria única. Degustaciones, masterclasses y la mejor gastronomía de la región.',
 'FESTIVAL',
 'Centro de Exposiciones y Convenciones',
 'Asunción',
 '2026-11-28 12:00:00-03',
 '2026-10-15 10:00:00-03',
 '2026-11-27 23:59:00-03',
 70000, 'PYG', 12000, 12000, NULL),

('a1000000-0000-0000-0000-000000000010',
 'Reveillon Asunción 2027',
 'La fiesta de fin de año más grande de la costanera. DJ internacionales, fuegos artificiales y la mejor vista del río Paraguay.',
 'FESTIVAL',
 'Anfiteatro Ñandutí',
 'Asunción',
 '2026-12-31 21:00:00-03',
 '2026-11-01 10:00:00-03',
 '2026-12-30 23:59:00-03',
 80000, 'PYG', 3500, 3500, NULL);

COMMIT;
