# **UPLINK Database Seed and Live Opportunity Ingestion Blueprint**

This blueprint partitions all 42 tables of the Uplink Supabase PostgreSQL schema into 38 active production tables and 4 future-scope or system-managed tables, supplies 66 verified live opportunity records collected across your 17 submitted links as of November 20, 2026, and provides schema-validated reference seed data and a topological ingestion prompt for Antigravity. Out of 16 unique portals after deduplicating the repeated InternDoor link, 7 portals yielded structured live opportunities—Unstop1, InternDoor5, AccioJob6, Hirist8, Google Careers10, JPMorganChase Oracle Cloud12, and Y Combinator Work at a Startup14—with expired postings from July through November 19, 2026 excluded.  
Two data boundaries apply before running database ingestion. First, 8 portals—NextRaise15, Jobgether (/talent/matches)17, ZipRecruiter India19, Skillmeet21, Wonderin22, CareerHub, HiringCafe23, and Built In24—are gated by authentication walls, client-side JavaScript rendering, or regional and ad-tracking redirects and returned no extractable Indian job rows, while DevConnect Platform exposed only program summary text without structured deadlines or stipends27. Second, external tables (companies, opportunities, opportunity\_skills, opportunity\_sources) contain live public postings verified from the source portals, whereas internal tables (users, profiles, applications, conversations, messages, posts, questions, assessments) use clearly labeled synthetic bootstrap and reference records so every foreign key and mobile screen works cleanly without fabricating real user identities.

## **Schema Audit: Active Modules vs. Future Scope**

Mapping all 42 tables across the nine schema domains identifies both the required parent-to-child insertion order and several schema constraints that must be satisfied during automated seeding. Specifically, experiences.start\_date is NOT NULL, recruiters.user\_id is UNIQUE (limiting each recruiter account to one company), recommendations.ref\_id is a polymorphic UUID that must resolve to an existing opportunities.id or skills.id, and opportunities stores stipend\_min and stipend\_max as NUMERIC(12,2) without a separate currency or period column—requiring consistent normalization (INR per month for internships and INR per annum CTC for full-time roles, documented explicitly in description).

| Domain | Table Name | Status | Parent Dependencies | Key Constraints and Seed Strategy |
| :---- | :---- | :---- | :---- | :---- |
| Core and Identity | roles | Active | None | name UNIQUE NOT NULL; seed 5 standard roles |
| Core and Identity | users | Active | None | email and phone UNIQUE; BCrypt $2a$10\$... password hashes required |
| Core and Identity | user\_roles | Active | users, roles | UNIQUE (user\_id, role\_id) (uq\_user\_role) |
| Core and Identity | user\_verifications | Active | users | type, document\_url NOT NULL; mix of APPROVED and PENDING |
| Core and Identity | admins | Active | users | user\_id UNIQUE NOT NULL |
| Profile and Academic | profiles | Active | users | user\_id UNIQUE NOT NULL |
| Profile and Academic | education | Active | users | degree, college, start\_year NOT NULL |
| Profile and Academic | skills | Active | None | name UNIQUE NOT NULL; canonical skill taxonomy |
| Profile and Academic | user\_skills | Active | users, skills | UNIQUE (user\_id, skill\_id) (uq\_user\_skill) |
| Profile and Academic | projects | Active | users | title NOT NULL |
| Profile and Academic | experiences | Active | users | title, company, start\_date NOT NULL |
| Profile and Academic | certifications | Active | users | title, issuer NOT NULL |
| Profile and Academic | achievements | Active | users | title NOT NULL |
| Opportunities and Apps | companies | Active | None | name NOT NULL; real employers extracted from live boards |
| Opportunities and Apps | recruiters | Active | users, companies | user\_id UNIQUE NOT NULL; 1 company per recruiter user |
| Opportunities and Apps | opportunities | Active | companies, users | type, title, company\_id, created\_by NOT NULL |
| Opportunities and Apps | opportunity\_skills | Active | opportunities, skills | UNIQUE (opportunity\_id, skill\_id) (uq\_opp\_skill) |
| Opportunities and Apps | opportunity\_sources | Active | opportunities | Tracks source\_name, source\_url, and fetched\_at |
| Opportunities and Apps | applications | Active | opportunities, users | UNIQUE (opportunity\_id, user\_id) (uq\_user\_opportunity) |
| Opportunities and Apps | application\_status\_history | Active | applications, users | status NOT NULL; chronological stage audit trail |
| Communication | conversations | Active | None | type in ('DIRECT', 'GROUP') |
| Communication | conversation\_members | Active | conversations, users | UNIQUE (conversation\_id, user\_id) (uq\_conv\_member) |
| Communication | messages | Active | conversations, users | content NOT NULL; message\_type in ('TEXT', 'FILE', 'LINK') |
| Communication | posts | Active | users | content NOT NULL; type in ('POST', 'BLOG', 'NOTICE') |
| Communication | comments | Active | posts, users | content NOT NULL |
| Communication | likes | Active | posts, users | UNIQUE (post\_id, user\_id) (uq\_post\_user\_like) |
| AI and Assessments | assessments | Active | None | title NOT NULL; is\_active \= TRUE |
| AI and Assessments | assessment\_questions | Active | assessments | options must be a valid JSON array string |
| AI and Assessments | assessment\_attempts | Active | users, assessments | Stores attempt timestamps and score |
| AI and Assessments | skill\_gaps | Active | users | target\_role NOT NULL; gap\_data valid JSON string |
| AI and Assessments | recommendations | Active | users (+ ref UUID) | ref\_id must match an existing opportunities.id or skills.id |
| Admin and System | notifications | Active | users | title, message NOT NULL |
| Admin and System | documents | Active | users | name, file\_url, type NOT NULL |
| Admin and System | ocr\_jobs | Future Scope | users | Keep empty (0 rows) so background OCR scheduler does not fail |
| Admin and System | audit\_logs | System Auto | users | Keep empty (0 rows) or insert 1 system seed marker |
| Bookmarks | saved\_opportunities | Active | users, opportunities | UNIQUE (user\_id, opportunity\_id) (uq\_user\_saved\_opportunity) |
| Q\&A Forum | questions | Active | users | Synchronize answer\_count, same\_doubt\_count, is\_unanswered |
| Q\&A Forum | answers | Active | questions, users | Synchronize helpful\_count with answer\_helpfuls |
| Q\&A Forum | question\_doubts | Active | questions, users | UNIQUE (question\_id, user\_id) (uq\_question\_user\_doubt) |
| Q\&A Forum | answer\_helpfuls | Active | answers, users | UNIQUE (answer\_id, user\_id) (uq\_answer\_user\_helpful) |
| Mentorship | mentor\_profiles | Future Scope | users (optional) | Empty (0 rows) default, or 1 is\_available \= FALSE placeholder |
| Mentorship | mentor\_bookings | Future Scope | mentor\_profiles, users | Strictly empty (0 rows) until mentorship launches |

## **Portal Audit and Verification Summary**

Each submitted link was verified for live, non-expired listings as of November 20, 2026\. The duplicate https\://interndoor.com/ entry in your request was merged into a single source pipeline, while dynamic SPAs, tracking URLs, and auth-gated platforms are flagged so Antigravity can target them later using browser automation or partner APIs.

| Submitted Portal URL | Extraction Status | Rows Included in Tables A–D | Blocker and Freshness Notes |
| :---- | :---- | :---- | :---- |
| https\://unstop.com/ | Active | 27 (14 hackathons/competitions, 13 internships) | Expired Jul–Nov 19 items removed; additional Nov hackathons exist on board1 |
| https\://interndoor.com/ (listed twice) | Active | 11 unique internships/trainee roles | Posted 7h–2d ago; merged 2 near-identical Zebra SWE Intern entries5 |
| https\://acciojob.com/ | Active | 14 (13 off-campus Full Stack roles \+ 1 training track) | Curated Oct 18–Nov 14 on placement.acciojob.com/jobs/6 |
| https\://www\.hirist.tech/k/mern-stack-jobs?ref=homepagetag | Active | 9 featured (20+ additional named roles in feed) | Predominantly 1–20 yrs experience; category URL used for apply path8 |
| https\://www\.google.com/about/careers/applications/jobs/results | Active (Partial) | 2 direct URLs (+1 Bengaluru title in sidebar) | Student Researcher open until Nov 27, 202610 |
| https\://jpmc.fa.oraclecloud.com/... (India SWE) | Active (Partial) | 2 India roles | Oracle Cloud HCM JS rendering limits deep listing extraction12 |
| https\://www\.workatastartup.com | Active (Partial) | 1 complete internship (FrontPage) | Job ID 110379 (Summer 2027\) has company/title behind YC auth14 |
| https\://devconnectplatform.com/program | Metadata Only | 0 | Describes work-assessed AI builder internship; no dates or stipend exposed27 |
| https\://nextraise.ai/ | Blocked (SPA) | 0 | Client-rendered aggregator (2M+ jobs claimed on site); no static rows15 |
| https\://jobgether.com/talent/matches | Blocked (Auth) | 0 | Personalized talent match feed requires login17 |
| https\://www\.ziprecruiter.in/ | Blocked / Redirect | 0 | Surfaced only US-domain ziprecruiter.com USD aggregate pages19 |
| https\://skillmeet.ai/ | Blocked (SPA) | 0 | Landing page only (28,575 active jobs claimed); JS-rendered21 |
| https\://wonderin.ai/ | No Job Feed | 0 | Surfaced only /resume-ai interview/resume tool22 |
| https\://www\.careerhub.com/multi-location-search?... | Blocked / Ad URL | 0 | Google Ads click-tracking URL with no extractable listings |
| https\://hiringcafe.com/ | Blocked (SPA) | 0 | Client-rendered aggregator at /jobs; requires headless browser23 |
| https\://builtin.com/jobs | Non-India / Category | 0 | Surfaced US, Glasgow, and Kuala Lumpur category pages only24 |

### **Excluded Expired and Incomplete Postings**

Exclude the following stale or incomplete records during database seeding:

> 1. **Expired Unstop Hackathons:** *13th Protocol: An AI Hackathon 2026* (closed Nov 18, 2026\)31, *CodeTitans India 2026* (ended Nov 17, 2026\)32, *CodeHack India* (closed Oct 27 / ended Nov 10, 2026\)33, and *Shakti Hackathon* (closed Oct 19, 2026\)34.  
> 2. **Expired Unstop Internships:** All July 2026 in-office internships (Domestify, Human Intelligence Labs, ECOFI, ZingMind, Salasar, AirAsia India, Miko.ai, DataCouch, CodeMore, Wooplix)35, all September 2026 listings (Skovio, ACA Technologies, Springer Capital)36, and short-window November postings that closed by November 19 (Skillpilot Solutions LLP, VSPH Technologies, Softius Internet Services, PaintOrbit Solutions, Spektra Systems)4.  
> 3. **Incomplete Listings:** *HackNowa Global Hackathon 2026* (missing dates and criteria)38, *Work at a Startup* job ID 110379 (missing company name and role title behind auth)29, and *SkillMe* software internship (borderline Nov 20 expiry with no disclosed stipend)4.

## **Live Verified Opportunities Dataset**

Ingest the parent companies and skills records before inserting opportunities, opportunity\_skills, and opportunity\_sources. For corporate homepages not directly present in the retrieved results, website uses the verified portal or career-page URL from the source data (or NULL pending manual enrichment) rather than unverified domain guesses.

### **Verified Employers (companies)**

| Name | Type | Website | Is Verified | Description |
| :---- | :---- | :---- | :---- | :---- |
| Google | Enterprise Tech | https\://www\.google.com/about/careers/applications/jobs/results | TRUE | Global tech company hiring AI/ML engineers in Bengaluru and student researchers10 |
| JPMorganChase | FinTech / Banking | https\://jpmc.fa.oraclecloud.com/hcmUI/CandidateExperience/en/sites/CX\_1001/jobs?location=India\&locationId=300000000289360\&locationLevel=country | TRUE | Global financial services firm hiring software engineers and Python analysts in India12 |
| Zebra Technologies | Enterprise Tech | https\://interndoor.com/ | TRUE | Employer listed on InternDoor for AI systems and SWE internships5 |
| Paytm | FinTech | https\://interndoor.com/ | TRUE | Indian payments and technology firm hiring backend interns in Noida/Remote5 |
| Thermo Fisher Scientific | Life Sciences / Tech | https\://interndoor.com/ | TRUE | Global scientific technology firm hiring IT backend interns5 |
| Honeywell Technologies | Industrial & AI | https\://interndoor.com/ | TRUE | Engineering firm hiring postgraduate AI interns5 |
| Baker Hughes | Energy Technology | https\://interndoor.com/ | TRUE | Energy technology firm hiring digital technology trainees5 |
| Emerson | Industrial Automation | https\://interndoor.com/ | TRUE | Industrial software employer hiring software engineer trainees5 |
| Cropin | AgriTech | https\://interndoor.com/ | TRUE | AgriTech company hiring quality assurance interns5 |
| Appinventiv | Software Services | https\://interndoor.com/ | TRUE | Software development firm hiring CS/IT software interns5 |
| HaystackAnalytics | HealthTech / Genomics | https\://interndoor.com/ | TRUE | Genomics analytics firm hiring genome variant analyst interns5 |
| pharma& | Healthcare / Pharma | https\://interndoor.com/ | TRUE | Pharmaceutical organization hiring PowerApps interns in Hyderabad5 |
| IIIT Lucknow | Academia | https\://unstop.com/ | TRUE | Indian Institute of Information Technology, Lucknow3 |
| IIT Bombay | Academia | https\://unstop.com/ | TRUE | Indian Institute of Technology Bombay, Powai1 |
| IIT Bhubaneswar | Academia | https\://unstop.com/ | TRUE | Indian Institute of Technology Bhubaneswar1 |
| NITK Surathkal | Academia | https\://unstop.com/ | TRUE | National Institute of Technology Karnataka, Surathkal1 |
| IIIT Allahabad | Academia | https\://unstop.com/ | TRUE | Indian Institute of Information Technology Allahabad1 |
| BIT Mesra | Academia | https\://unstop.com/ | TRUE | Birla Institute of Technology, Mesra, Ranchi1 |
| K. J. Somaiya College of Engineering | Academia | https\://unstop.com/ | TRUE | Engineering school in Vidyavihar, Mumbai1 |
| Zeliot Pvt Ltd | Mobility SaaS | https\://unstop.com/ | TRUE | Host of Configure 2026 mobility hackathon1 |
| Mondelēz International | FMCG | https\://unstop.com/ | TRUE | Global consumer goods firm hosting Maestros 20261 |
| BuildX | Tech Community | https\://unstop.com/ | FALSE | Organizer of CodeWarriors India 2026 National Hackathon2 |
| HackWithPune | Tech Community | https\://unstop.com/ | FALSE | Organizer of CodeUnited India National Hackathon1 |
| DevHarbour | Tech Community | https\://unstop.com/ | FALSE | Organizer of INNOVATE Hackathon 202640 |
| EduLinkUp | Tech Community | https\://unstop.com/ | FALSE | Organizer of ELU Build hackathon1 |
| Riwa Creatives | Event Organizer | https\://unstop.com/ | FALSE | Organizer of Hack Uttar Pradesh 20261 |
| Unstop | Career Platform | https\://unstop.com/ | TRUE | Student competition, hackathon, and internship platform1 |
| AccioJob | EdTech & Placement | https\://acciojob.com/ | TRUE | Full-stack training and off-campus job curation platform6 |
| FrontPage | Startup (YC) | https\://www\.workatastartup.com/jobs/122543 | TRUE | Startup hiring engineering interns via YC Work at a Startup14 |
| GlobalLogic | Digital Engineering | https\://www\.hirist.tech/k/mern-stack-jobs | TRUE | Engineering firm hiring MERN stack developers in Hyderabad8 |
| Amadeus Labs | Travel Tech | https\://www\.hirist.tech/c/full-stack-jobs | TRUE | Travel technology R\&D center hiring SDE III Full Stack in Bengaluru9 |
| Mphasis | IT Services | https\://www\.hirist.tech/c/full-stack-jobs | TRUE | IT services provider hiring Senior Full Stack AI Engineers9 |
| Publicis Sapient | Digital Consulting | https\://www\.hirist.tech/c/full-stack-jobs | TRUE | Digital consultancy hiring Python Full Stack Agentic AI developers9 |
| NatWest | FinTech / Banking | https\://www\.hirist.tech/c/full-stack-jobs | TRUE | Banking technology hub hiring Principal Engineers in Gurugram9 |
| Procore | Construction SaaS | https\://www\.hirist.tech/c/full-stack-jobs | TRUE | SaaS firm hiring Staff Full Stack Engineers in Bengaluru9 |

Insert the remaining named employers from Unstop (Coy, Vortizo AI, Ubisar, Slonix Solution Private Limited, Zenotalent, Codeatrix, IntelleQAcademy, Nexicat, EduGuruCool Operating Systems Private Limited, TrutiAI, Tringflow Private Limited)4, AccioJob (Requin Solutions Private Limited, Young Minds Technology Solutions Pvt Ltd, Kriscent Techno Hub Pvt. Ltd., CyberClipper InfoTech LLP, ImaginXP, Solvative, Zecser Business LLP, BizTranSights, Ascendion, Vedak, Brackets, Expinator Web Technology (P) Ltd., Developer bazaar Technologies)7, and Hirist (Cumulations Technologies, DataPure, Euphoric Thought)8 using the same schema with is\_verified \= FALSE (or TRUE for established firms once verified) and website \= NULL until domain verification runs.

### **Master Technical Skills (skills)**

Insert these canonical skills once (ON CONFLICT (name) DO NOTHING) using only skills explicitly evidenced across the scraped listings or required by the active assessments:

* **Languages:** Python, Java, JavaScript, TypeScript, C++, C\#, SQL, HTML, CSS5  
* **Frontend and Mobile:** React.js, AngularJS, React Native7  
* **Backend and Databases:** Node.js, Express.js, Spring Boot, FastAPI, .NET Core, Ruby on Rails, MongoDB, PostgreSQL, MySQL, REST APIs, Microservices Architecture, System Design, Distributed Systems5  
* **AI, Cloud and Engineering Tools:** Machine Learning, Large Language Models (LLMs), Natural Language Processing (NLP), Generative AI, Agentic AI, Prompt Engineering, TensorFlow, PyTorch, Databricks, AWS, Docker, CI/CD Pipelines, Git, Selenium, PowerApps, Data Analysis, Data Structures and Algorithms2

### **Table A: Live Unstop Hackathons and Competitions**

All 14 selections below remain active on or after November 20, 2026 (source\_name \= 'Unstop')1. Deadlines marked with \~ are calculated from the listing's posted/updated date and remaining countdown days1; AI Kaushal Hackathon 2026 (restricted to Uttar Pradesh residents) and Configure 2026 close on November 20, 20261, while INNOVATE \- Hackathon 2026 lists a December 6 registration deadline alongside earlier Round 1 submission dates (Nov 3–17)40. Note that stipend\_min and stipend\_max remain NULL for competitions; prize pools and internship/PPO tracks go in description.

| Company | Type | Title | Location | Work Mode | Deadline | Application URL | Skills Explicitly Stated in Listing |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| IIIT Lucknow | HACKATHON | AI Kaushal Hackathon 2026 | Lucknow, UP | HYBRID | 2026-11-20 | https\://unstop.com/hackathons/ai-kaushal-hackathon-2026-indian-institute-of-information-technology-iiit-lucknow-1817798 | Applied AI (Machine Learning, Generative AI)3 |
| BuildX | HACKATHON | CodeWarriors India 2026 \- National Hackathon | India | REMOTE | 2026-12-03 | https\://unstop.com/hackathons/codewarriors-india-2026-national-hackathon-buildx-1817737 | Agentic AI, Full-Stack Product Engineering2 |
| HackWithPune | HACKATHON | CodeUnited India \- National Hackathon | India | REMOTE | \~2026-12-01 | https\://unstop.com/hackathons/codeunited-india-national-hackathon-hackwithpune-1817749 | Agentic AI, Full-Stack1 |
| DevHarbour | HACKATHON | INNOVATE \- Hackathon 2026 | India | REMOTE | 2026-12-06 | https\://unstop.com/hackathons/innovate-hackathon-2026-devharbour-1792025 | None specified (AI credits reward)40 |
| NITK Surathkal | HACKATHON | Silicon Sprint 2026 | Surathkal | REMOTE | \~2026-11-23 | https\://unstop.com/hackathons | None specified1 |
| IIIT Allahabad | HACKATHON | OpenCode 2026 | Prayagraj | REMOTE | \~2026-12-11 | https\://unstop.com/hackathons | None specified1 |
| BIT Mesra | COMPETITION | CTF Cicada 2026 | Ranchi | REMOTE | \~2026-12-08 | https\://unstop.com/hackathons | None specified1 |
| IIT Bhubaneswar | HACKATHON | Code Relay 5.0 | Bhubaneswar | ONSITE | \~2026-12-10 | https\://unstop.com/hackathons | None specified1 |
| K. J. Somaiya College of Engineering | HACKATHON | Agri-Tech Hackathon 2027: 24 Hours to Hack the Harvest | Mumbai, MH | ONSITE | \~2026-12-12 | https\://unstop.com/hackathons | None specified1 |
| IIT Bombay | COMPETITION | The Algo Trading Innovation Challenge | Mumbai, MH | ONSITE | \~2026-11-24 | https\://unstop.com/hackathons | None specified1 |
| Zeliot Pvt Ltd | HACKATHON | Configure 2026: A Mobility Hackathon | India | REMOTE | \~2026-11-20 | https\://unstop.com/hackathons | None specified1 |
| EduLinkUp | HACKATHON | ELU Build | India | REMOTE | \~2026-12-15 | https\://unstop.com/hackathons | Software Development1 |
| Riwa Creatives | HACKATHON | Hack Uttar Pradesh 2026 | Uttar Pradesh | ONSITE | \~2026-12-17 | https\://unstop.com/hackathons | AI Engineering1 |
| Mondelēz International | COMPETITION | Mondelēz Maestros 2026 \- Commercial Track | India | REMOTE | \~2026-12-12 | https\://unstop.com/hackathons | Management / Commercial Track1 |

### **Table B: Live Unstop Paid and Technical Internships**

All 13 internships below use monthly INR values in stipend\_min and stipend\_max (NULL where undisclosed), with deadlines derived from posted dates and countdowns in Unstop's November 2026 feed4. Only skills explicitly indicated in the role titles are mapped so unverified stack tags are not assumed.

| Company | Title | Location | Work Mode | Stipend Min–Max (INR/mo) | Approx. Deadline | Application URL | Title-Evidenced Skills |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| Coy | Software Engineer Intern | Gurgaon, HR | HYBRID | 45000.00 – 45000.00 | \~2026-11-23 | https\://unstop.com/internship/software-development-internship | None specified4 |
| Vortizo AI | Python Developer Internship | Remote (India) | REMOTE | 40000.00 – 40000.00 | \~2026-11-22 | https\://unstop.com/internship/software-development-internship | Python4 |
| Ubisar | Tech Internship | Remote (India) | REMOTE | 30000.00 – 30000.00 | \~2026-11-23 | https\://unstop.com/internship/software-development-internship | None specified4 |
| Slonix Solution Private Limited | Software Developer Internship | Bengaluru, KA | ONSITE | 18000.00 – 30000.00 | \~2026-11-29 | https\://unstop.com/internship/software-development-internship | None specified4 |
| Zenotalent | Python Internship | Remote (India) | REMOTE | 25000.00 – 25000.00 | \~2026-11-22 | https\://unstop.com/internship/software-development-internship | Python4 |
| Codeatrix | Java Developer Internship | Remote (India) | REMOTE | 15000.00 – 20000.00 | \~2026-12-09 | https\://unstop.com/internship/software-development-internship | Java4 |
| IntelleQAcademy | Python Development Internship | Remote (India) | REMOTE | 10000.00 – 15000.00 | \~2026-11-22 | https\://unstop.com/internship/software-development-internship | Python4 |
| IntelleQAcademy | Java Development Internship | Remote (India) | REMOTE | 10000.00 – 15000.00 | \~2026-11-22 | https\://unstop.com/internship/software-development-internship | Java4 |
| Nexicat | Software Development Engineer Internship | Remote (India) | REMOTE | 5000.00 – 5000.00 | \~2026-11-23 | https\://unstop.com/internship/software-development-internship | None specified4 |
| EduGuruCool Operating Systems Private Limited | Product & Technology Internship | Pan India | REMOTE | 2000.00 – 2000.00 | \~2026-11-26 | https\://unstop.com/internship/software-development-internship | None specified4 |
| TrutiAI | Software Development and Technical Writing Internship | Remote (India) | REMOTE | NULL – NULL | \~2026-11-30 | https\://unstop.com/internship/software-development-internship | None specified4 |
| Unstop | Content Development Intern \- Tech | South Delhi | HYBRID | NULL – NULL | \~2026-12-16 | https\://unstop.com/internship/software-development-internship | None specified4 |
| Tringflow Private Limited | Software Internship | Remote (India) | REMOTE | NULL – NULL | \~2026-11-23 | https\://unstop.com/internship/software-development-internship | None specified4 |

### **Table C: Live InternDoor Engineering Board Roles**

All 11 unique listings below appeared live on https\://interndoor.com/ within 7 hours to 2 days of retrieval (source\_name \= 'InternDoor')5. Two near-identical Zebra Technologies Software Engineering Intern entries were consolidated into one 6-month hybrid record.

| Company | Type | Title | Location | Work Mode | Experience Level | Application URL | Explicitly Listed Skills |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| Zebra Technologies | INTERNSHIP | Intern II \- AI Systems Development | India | HYBRID | Fresher / Student | https\://interndoor.com/ | Large Language Models (LLMs), Natural Language Processing (NLP), REST APIs, CI/CD Pipelines, Git5 |
| Paytm | INTERNSHIP | Backend Intern | Noida, UP | REMOTE | Fresher / Student | https\://interndoor.com/ | None specified5 |
| Thermo Fisher Scientific | INTERNSHIP | IT Intern \- Backend Development | India | ONSITE | UG / PG B.Tech | https\://interndoor.com/ | Python, AWS, SQL, Git, REST APIs, CI/CD Pipelines5 |
| Honeywell Technologies | INTERNSHIP | AI Intern (Algorithm Development) | India | ONSITE | Postgraduate | https\://interndoor.com/ | Python, TensorFlow, PyTorch5 |
| Zebra Technologies | INTERNSHIP | Software Engineering Intern (6 Months) | India | HYBRID | Pursuing CS / DS / Stats | https\://interndoor.com/ | None specified (Microsoft Office / coding)5 |
| Appinventiv | INTERNSHIP | Software Intern | India | ONSITE | Pursuing CS / IT | https\://interndoor.com/ | Java, Python, Git5 |
| pharma& | INTERNSHIP | PowerApps Developer Intern (6 Months) | Hyderabad, TS | ONSITE | UG / PG CS | https\://interndoor.com/ | PowerApps5 |
| Baker Hughes | TRAINING | Digital Technology Trainee (6–12 Months) | India | ONSITE | UG Engineering | https\://interndoor.com/ | None specified (Modeling Software / PLM)5 |
| Cropin | INTERNSHIP | Quality Assurance Intern | India | ONSITE | UG BCA / MCA / BE | https\://interndoor.com/ | Selenium, Java, Python, SQL5 |
| HaystackAnalytics | INTERNSHIP | Genome Variant Analyst Intern | India | ONSITE | PG (0–1 yrs exp) | https\://interndoor.com/ | Data Analysis (NGS / Genomics)5 |
| Emerson | JOB | Software Engineer Trainee | India | ONSITE | UG / PG B.Tech / M.Tech | https\://interndoor.com/ | C\#, C++, Java, Python5 |

### **Table D: Live Direct Corporate, YC Startup, AccioJob, and Hirist Roles**

This batch spans 2 Google Careers listings10, 2 JPMorganChase India listings12, 1 Y Combinator Work at a Startup internship14, all 13 off-campus Full Stack roles plus 1 self-paced GenAI training track from AccioJob6, and 9 featured named-employer roles from Hirist8. Where a listing does not state work\_mode explicitly, ONSITE is recorded as the schema default and noted in description.

| Company | Source Portal | Type | Title | Location | Work Mode | Experience Level | Compensation / Window | Application URL | Explicitly Listed Skills |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| Google | Google Careers | JOB | Software Engineer, AI/ML, PhD, Early Career, 2026 | Bengaluru, KA | ONSITE | Early Career (PhD) | Undisclosed / Rolling | https\://www\.google.com/about/careers/applications/jobs/results/127602843300307654-software-engineer-aiml-phd-early-career-2026 | Machine Learning (AI/ML)11 |
| Google | Google Careers | RESEARCH | Student Researcher, BS/MS, Fall 2026 | Global / US | ONSITE | BS / MS Student | Deadline 2026-11-27 | https\://www\.google.com/about/careers/applications/jobs/results/132362676918461126-student-researcher-bsms-fall-2026 | Python, Java, JavaScript, C++, Machine Learning, Natural Language Processing (NLP)10 |
| JPMorganChase | JPMC Oracle Cloud | JOB | Software Engineer III (Consumer) | India | ONSITE | Mid-Senior | Undisclosed / Rolling | https\://jpmc.fa.oraclecloud.com/hcmUI/CandidateExperience/en/sites/CX\_1001/job/210808578 | None specified12 |
| JPMorganChase | JPMC Oracle Cloud | JOB | Python Developer (Analyst) \- STO | India | ONSITE | Analyst | Undisclosed / Rolling | https\://jpmc.fa.oraclecloud.com/hcmUI/CandidateExperience/en/sites/CX\_1001/jobs?location=India\&locationId=300000000289360\&locationLevel=country | Python, FastAPI, React.js, Databricks13 |
| FrontPage | Work at a Startup | INTERNSHIP | Engineering Intern: Testing, Automation & Development | Undisclosed | ONSITE | Student / Intern | Undisclosed / Rolling | https\://www\.workatastartup.com/jobs/122543 | None specified (Testing, Automation)14 |
| AccioJob | AccioJob | TRAINING | MERN Full Stack Development With GenAI | India | REMOTE | All Backgrounds | Course fee ₹55,0006 | https\://acciojob.com/self-paced | JavaScript, React.js, Node.js, Express.js, MongoDB, Generative AI6 |
| Requin Solutions Private Limited | AccioJob Board | INTERNSHIP | Full Stack Developer | Jaipur, RJ | ONSITE | Fresher | Posted Nov 14, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | React.js7 |
| CyberClipper InfoTech LLP | AccioJob Board | INTERNSHIP | Full Stack Developer | Dehradun, UK | ONSITE | Fresher | Posted Nov 4, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | MongoDB, Express.js, React.js, Node.js, HTML, CSS, JavaScript7 |
| Zecser Business LLP | AccioJob Board | INTERNSHIP | Full Stack Developer | Kochi, KL | ONSITE | Fresher | Posted Oct 26, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | Node.js, React.js, MongoDB, Express.js7 |
| BizTranSights | AccioJob Board | INTERNSHIP | Full Stack Developer | Nagpur, MH | ONSITE | Fresher | Posted Oct 26, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | MongoDB, Express.js, React.js, Node.js7 |
| Vedak | AccioJob Board | INTERNSHIP | Full Stack Developer | Karnataka | ONSITE | Fresher | Posted Oct 22, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | CSS, HTML, JavaScript, MySQL, Node.js, React.js7 |
| Young Minds Technology Solutions Pvt Ltd | AccioJob Board | JOB | Full Stack Developer | Tirupati, AP | ONSITE | Fresher | Posted Nov 13, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | React.js, MongoDB7 |
| Kriscent Techno Hub Pvt. Ltd. | AccioJob Board | JOB | Full Stack Developer | Kota, RJ | ONSITE | Fresher | Posted Nov 13, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | React.js, Node.js, Express.js7 |
| ImaginXP | AccioJob Board | JOB | Full Stack Developer | Gujarat | ONSITE | Fresher | Posted Nov 3, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | Java, Node.js, SQL, HTML, CSS, JavaScript7 |
| Solvative | AccioJob Board | JOB | Full Stack Developer | Gujarat | ONSITE | Fresher | Posted Oct 31, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | HTML, CSS, JavaScript, React.js7 |
| Ascendion | AccioJob Board | JOB | Full Stack Developer | India | ONSITE | Fresher | Posted Oct 24, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | Java, SQL, Spring Boot7 |
| Brackets | AccioJob Board | JOB | Full Stack Developer | West Bengal | ONSITE | Fresher | Posted Oct 21, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | Node.js, React.js, JavaScript, SQL7 |
| Expinator Web Technology (P) Ltd. | AccioJob Board | JOB | Full Stack Developer | Mohali, PB | ONSITE | Fresher | Posted Oct 21, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | HTML, React.js, Node.js, MongoDB, Java7 |
| Developer bazaar Technologies | AccioJob Board | JOB | Full Stack Developer | Indore, MP | ONSITE | Fresher | Posted Oct 18, 2026 | https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other | HTML, CSS, JavaScript, React.js7 |
| Cumulations Technologies | Hirist | JOB | Full Stack Developer \- MERN Stack | Bengaluru, KA | ONSITE | 1–3 yrs | Undisclosed (Posted \~1 mo) | https\://www\.hirist.tech/k/mern-stack-jobs | MongoDB, Express.js, React.js, Node.js8 |
| DataPure | Hirist | JOB | MEAN/MERN Stack Developer | Indore, MP | ONSITE | 3–5 yrs | Undisclosed (Posted \~3d) | https\://www\.hirist.tech/k/mern-stack-jobs | MongoDB, Express.js, React.js, Node.js8 |
| Euphoric Thought | Hirist | JOB | MERN Stack Developer | Bengaluru, KA | ONSITE | 4–5 yrs | Undisclosed (Posted \~2w) | https\://www\.hirist.tech/k/mern-stack-jobs | MongoDB, Express.js, React.js, Node.js8 |
| GlobalLogic | Hirist | JOB | MERN Stack Developer \- React.js/Node.js | Hyderabad, TS | ONSITE | 5–10 yrs | Undisclosed (Posted \~1w) | https\://www\.hirist.tech/k/mern-stack-jobs | TypeScript, React.js, Node.js, MongoDB8 |
| Publicis Sapient | Hirist | JOB | Python Full Stack Agentic AI Developer | Multiple (India) | HYBRID | 5–15 yrs | Undisclosed (Posted \~2d) | https\://www\.hirist.tech/c/full-stack-jobs | Python, Agentic AI9 |
| Amadeus Labs | Hirist | JOB | Software Development Engineer III \- Full Stack | Bengaluru, KA | ONSITE | 7–10 yrs | Undisclosed (Posted Nov 2026\) | https\://www\.hirist.tech/c/full-stack-jobs | Java9 |
| Mphasis | Hirist | JOB | Senior Full Stack AI Engineer | Bengaluru / Hyderabad | HYBRID | 6–9 yrs | Undisclosed (Posted \~1d) | https\://www\.hirist.tech/c/full-stack-jobs | Java9 |
| NatWest | Hirist | JOB | Principal Engineer \- .Net/React.js | Gurugram, HR | HYBRID | 12–20 yrs | 3500000.00 – 5000000.00 INR/yr | https\://www\.hirist.tech/c/full-stack-jobs | C\#, .NET Core, React.js, Agentic AI, Prompt Engineering9 |
| Procore | Hirist | JOB | Staff Software Engineer \- Full Stack Development | Bengaluru, KA | ONSITE | 9–11 yrs | Undisclosed (Posted \~6d) | https\://www\.hirist.tech/c/full-stack-jobs | System Design, Distributed Systems, Ruby on Rails, React.js, AngularJS9 |

## **Active Non-Opportunity Modules: Structured Reference Seed Data**

Because private user profiles, applications, chat threads, and quiz records are internal application state rather than public portal data, the specifications below provide synthetic bootstrap and reference seed records designed for Indian engineering campus workflows and your Spring Boot, React Native, and AI stack. Always store valid BCrypt hashes ($2a$10\$...) for users.password and resolve foreign keys dynamically via email or name lookups.

### **Core Identity, Admin, Recruiters, and Verifications**

| Table | Reference Bootstrap Records |
| :---- | :---- |
| roles | ROLE\_STUDENT, ROLE\_RECRUITER, ROLE\_ADMIN, ROLE\_FACULTY, ROLE\_ALUMNI |
| users | admin@uplink.edu ("Uplink Platform Admin", verified), ingestor@uplink.edu ("Uplink Opportunity Sync", verified), recruiter@uplink.edu ("Ananya Deshmukh", verified), student@uplink.edu ("Rohan Kulkarni", verified), peer1@uplink.edu ("Sneha Nair", verified), peer2@uplink.edu ("Karthik Subramanian", unverified); assign distinct phones or NULL |
| user\_roles | Map admin@uplink.edu and ingestor@uplink.edu to ROLE\_ADMIN; recruiter@uplink.edu to ROLE\_RECRUITER; student@uplink.edu, peer1@uplink.edu, peer2@uplink.edu to ROLE\_STUDENT |
| admins | admin@uplink.edu (Placement & Industry Relations) and ingestor@uplink.edu (Automated Opportunity Pipeline) |
| recruiters | recruiter@uplink.edu linked to Google (University Talent Partner, is\_verified \= TRUE); keep 1 company per recruiter user due to user\_id UNIQUE |
| user\_verifications | student@uplink.edu: COLLEGE\_ID, APPROVED (verified by admin@uplink.edu); peer1@uplink.edu: IDENTITY\_PROOF, APPROVED; peer2@uplink.edu: COLLEGE\_ID, PENDING (verified\_by \= NULL) |

### **Student Portfolios and Academic Records**

| Table | Reference Seed Values (student@uplink.edu, peer1@uplink.edu, peer2@uplink.edu) |
| :---- | :---- |
| profiles | student@uplink.edu: \`"Final-Year B.Tech CSE |
| education | student@uplink.edu: "B.Tech", "Vishwakarma Institute of Technology, Pune", "Computer Science & Engineering", 2023–2027, cgpa \= 8.92, is\_current \= TRUE; peer1@uplink.edu: "B.Tech", "RV College of Engineering, Bengaluru", "AI & Machine Learning", 2023–2027, cgpa \= 9.15, is\_current \= TRUE; peer2@uplink.edu: "B.E.", "CBIT Hyderabad", "Information Technology", 2024–2028, cgpa \= 8.40, is\_current \= TRUE |
| user\_skills | student@uplink.edu: Java (ADVANCED, 2.0 yrs, verified), Spring Boot (INTERMEDIATE, 1.5 yrs, verified), React Native (INTERMEDIATE, 1.5 yrs), TypeScript (INTERMEDIATE, 1.5 yrs), PostgreSQL (INTERMEDIATE, 1.0 yrs, verified); peer1@uplink.edu: Python (EXPERT, 2.5 yrs, verified), FastAPI (ADVANCED, 1.5 yrs), Machine Learning (INTERMEDIATE, 1.0 yrs) |
| projects | student@uplink.edu: "Campus Placement & STOMP Messaging Hub" (tech\_stack \= 'Spring Boot, React Native, PostgreSQL, WebSockets'); peer1@uplink.edu: "ATS Resume Semantic Scorer" (tech\_stack \= 'Python, FastAPI, PostgreSQL') |
| experiences | student@uplink.edu: "Backend Engineering Intern", "FinEdge Systems", "Pune, Maharashtra", start\_date \= '2026-05-15', end\_date \= '2026-07-31' |
| certifications | student@uplink.edu: "AWS Certified Cloud Practitioner" (issuer \= 'Amazon Web Services', issue\_date \= '2026-03-10'); peer1@uplink.edu: "Deep Learning Specialization" (issuer \= 'DeepLearning.AI', issue\_date \= '2026-01-20') |
| achievements | student@uplink.edu: "Smart India Hackathon Finalist" (date \= '2025-12-18'); peer1@uplink.edu: "Top 5% Kaggle Tabular Playground" (date \= '2026-04-05') |

### **Applications, Status History, and Bookmarks**

| Table | Reference Seed Values and Foreign-Key Links |
| :---- | :---- |
| applications | Seed 3 applications for student@uplink.edu: (1) Zebra Technologies \- Intern II \- AI Systems Development (SHORTLISTED, expected\_ctc \= 35000.00); (2) Paytm \- Backend Intern (UNDER\_REVIEW, expected\_ctc \= 30000.00); (3) JPMorganChase \- Python Developer (Analyst) \- STO (APPLIED, expected\_ctc \= 1400000.00) |
| application\_status\_history | Zebra (SHORTLISTED): 3 chronological rows (APPLIED \-\> UNDER\_REVIEW \-\> SHORTLISTED, latter two changed by recruiter@uplink.edu); Paytm: 2 rows (APPLIED \-\> UNDER\_REVIEW); JPMC: 1 row (APPLIED) |
| saved\_opportunities | Bookmark CodeWarriors India 2026 \- National Hackathon and Coy \- Software Engineer Intern for student@uplink.edu, and Thermo Fisher Scientific \- IT Intern \- Backend Development for peer1@uplink.edu |

### **Real-Time Chat and Community Feed**

| Table | Reference Seed Values for Multi-User Testing |
| :---- | :---- |
| conversations | Conv 1 (DIRECT: student@uplink.edu and peer1@uplink.edu), Conv 2 (DIRECT: student@uplink.edu and recruiter@uplink.edu), Conv 3 (GROUP: student@uplink.edu, peer1@uplink.edu, peer2@uplink.edu) |
| conversation\_members | Insert participant pairs and trio with ADMIN for conversation creator and MEMBER for peers |
| messages | Conv 1: 4 alternating TEXT messages coordinating a CodeWarriors India 2026 team (final message from peer1@uplink.edu has is\_read \= FALSE to populate unread badge \= 1); Conv 2: 2 read messages on interview prep; Conv 3: 2 read group messages |
| posts | NOTICE by admin@uplink.edu ("AI Kaushal Hackathon 2026 closes Nov 20; CodeWarriors India 2026 open until Dec 3."), POST by peer1@uplink.edu ("Looking for 1 React Native and 1 Spring Boot teammate for CodeWarriors India 2026 Agentic AI track."), BLOG by recruiter@uplink.edu ("What technical interviewers look for in backend and AI internships.") |
| comments and likes | 2 comments on the POST (student@uplink.edu, peer2@uplink.edu) and 4 unique likes rows respecting uq\_post\_user\_like |

### **Assessments, Skill Gaps, and Recommendations**

| Table | Reference Seed Values and JSON Payloads |
| :---- | :---- |
| assessments | 3 active quizzes (is\_active \= TRUE): "Backend Engineering: Java, Spring Boot & PostgreSQL" (Backend, 20 min), "Full-Stack MERN & TypeScript Fundamentals" (Full Stack, 20 min), "Applied AI, LLMs & RAG Architecture" (AI/ML, 15 min) |
| assessment\_questions | 5 multiple-choice questions per quiz (15 total), storing options as valid JSON arrays (e.g., '\["Optimistic locking with @Version", "Table-level exclusive lock", "Disabling connection pooling", "Using bytea columns"\]'), correct\_answer matching one option verbatim, marks \= 1.0, order\_index \= 1..5 |
| assessment\_attempts | Completed attempts for student@uplink.edu (Backend quiz, score \= 4.00) and peer1@uplink.edu (AI/ML quiz, score \= 5.00) |
| skill\_gaps | 1 record for student@uplink.edu (target\_role \= 'Full-Stack & AI Systems Intern') with gap\_data JSON listing missing\_skills (\["Docker", "CI/CD Pipelines", "Large Language Models (LLMs)"\]) and study steps |
| recommendations | 3 rows for student@uplink.edu: 2 OPPORTUNITY rows whose ref\_id equals the exact UUIDs of Zebra Technologies \- Intern II \- AI Systems Development (91.50) and Codeatrix \- Java Developer Internship (88.00), plus 1 SKILL row referencing the Large Language Models (LLMs) skill UUID (85.00) |

### **Peer Q\&A Forum, Notifications, and Documents**

| Table | Reference Seed Values and Counter Synchronization |
| :---- | :---- |
| questions | 3 questions: Q1 by student@uplink.edu ("How to avoid Hibernate 7 lower(bytea) errors with nullable search filters in PostgreSQL?", answer\_count \= 1, same\_doubt\_count \= 2, is\_unanswered \= FALSE); Q2 by peer2@uplink.edu ("Should freshers apply to Hirist MERN jobs requiring 1–3 yrs experience?", answer\_count \= 1, same\_doubt\_count \= 1, is\_unanswered \= FALSE); Q3 by peer1@uplink.edu ("Best deployment setup for STOMP /ws-raw on 512MB vs 2GB containers?", answer\_count \= 0, same\_doubt\_count \= 1, is\_unanswered \= TRUE) |
| answers | 2 answers covering Q1 (is\_accepted \= TRUE, helpful\_count \= 2\) and Q2 (is\_accepted \= FALSE, helpful\_count \= 1\) |
| question\_doubts and answer\_helpfuls | Exact child rows matching same\_doubt\_count (2, 1, 1\) and helpful\_count (2, 1\) so counts stay mathematically exact |
| notifications and documents | 3 notifications for student@uplink.edu (2 unread, 1 read) and 2 documents (RESUME and IDENTITY records) |

## **Future-Scope Modules: Empty State and Placeholder Specification**

Because mentorship is deferred to future scope and background OCR/audit workflows depend on automated triggers, handle these four tables cleanly so no background errors or misleading cards appear in production.

| Table | Recommended Default | Optional Placeholder Convention (Only If Non-Empty State Is Needed) | Engineering Rationale |
| :---- | :---- | :---- | :---- |
| mentor\_profiles | Empty (0 rows) via TRUNCATE | 1 single row: name \= '\[FUTURE SCOPE\] Mentorship Launching Soon', designation \= 'Coming Soon', organization \= 'Uplink Mentorship Program', bio \= 'FUTURE\_SCOPE\_PLACEHOLDER: Industry mentor onboarding opens in a future release.', is\_available \= FALSE, rating \= 0.00, sessions\_completed \= 0 | Prevents fake mentors in the app; is\_available \= FALSE blocks booking attempts if a visible banner row is used |
| mentor\_bookings | Strictly empty (0 rows) | Keep empty (0 rows) even when a placeholder mentor exists | Avoids phantom scheduled sessions on the student profile |
| ocr\_jobs | Strictly empty (0 rows) | Do not insert any PENDING placeholder rows | Spring Boot's BackgroundJobScheduler polls ocr\_jobs every 60 seconds; a fake PENDING URL will repeatedly throw processing failures |
| audit\_logs | Empty (0 rows) | Optional: 1 marker row with action \= 'SEED\_INITIALIZED', entity \= 'SYSTEM', metadata \= '{"scope":"v1\_initial\_seed","future\_scope":\["mentorship","ocr\_pipeline"\]}' | Leaves a clean audit marker without polluting user activity logs |

## **Parent-to-Child Ingestion Order and Antigravity Execution Prompt**

Ingest records in six strict topological tiers using PostgreSQL CTE SELECT id lookups and ON CONFLICT DO NOTHING so foreign keys and unique constraints never collide:

> 1. **Tier 1 (Root Entities):** roles, users, skills, companies, assessments, conversations.  
> 2. **Tier 2 (Identity and Profiles):** user\_roles, admins, user\_verifications, profiles, education, user\_skills, projects, experiences, certifications, achievements, recruiters, assessment\_questions, conversation\_members.  
> 3. **Tier 3 (Opportunities and Content):** opportunities, posts, questions, documents.  
> 4. **Tier 4 (Junctions and Activity):** opportunity\_skills, opportunity\_sources, comments, likes, answers, question\_doubts, messages, applications, saved\_opportunities, assessment\_attempts, skill\_gaps, recommendations, notifications.  
> 5. **Tier 5 (Leaf Audits and Feedback):** application\_status\_history, answer\_helpfuls.  
> 6. **Tier 6 (Future-Scope Cleanup):** Verify mentor\_bookings and ocr\_jobs contain 0 rows and mentor\_profiles is either empty or holds only the single is\_available \= FALSE future-scope banner row.

Paste this prompt directly into Antigravity to execute the full ingestion against Supabase:  
Generate and execute an idempotent Supabase PostgreSQL seed script (and update DataSeeder if needed so it does not re-insert legacy test rows) for **Uplink** using the 42-table schema and the verified dataset in this blueprint.

### **Ingestion Requirements**

> 1. **Clean Reset First:** Truncate all 42 tables (TRUNCATE TABLE ... RESTART IDENTITY CASCADE) so legacy test rows ("Test Job", "Validation automated post", old dummy mentors) are completely wiped.  
> 2. **Topological Order & Idempotency:** Insert strictly in Tiers 1 \-\> 6 using CTE SELECT id lookups and ON CONFLICT DO NOTHING so all unique constraints (uq\_user\_role, uq\_user\_skill, uq\_opp\_skill, uq\_user\_opportunity, uq\_conv\_member, uq\_post\_user\_like, uq\_question\_user\_doubt, uq\_answer\_user\_helpful) pass cleanly.  
> 3. **Schema & Counter Integrity:**  
   * Use valid BCrypt $2a$10\$... hashes for users.password.  
   * Ensure experiences.start\_date and education.start\_year are non-null, assessment\_questions.options is a JSON array string, and skill\_gaps.gap\_data is a valid JSON object string.  
   * Resolve recommendations.ref\_id to real inserted opportunities.id or skills.id UUIDs.  
   * Keep questions.answer\_count, questions.same\_doubt\_count, questions.is\_unanswered, and answers.helpful\_count exactly equal to their child rows.  
> 4. **Live Opportunities Accuracy:**  
   * Ingest all 66 verified, non-expired live opportunities from Unstop, InternDoor, AccioJob, Hirist, Google Careers, JPMorganChase, and YC Work at a Startup, excluding all expired Jul–Nov 19 listings.  
   * Map portal names and source URLs in opportunity\_sources and link verified skills in opportunity\_skills.  
   * Specify compensation units clearly in opportunities.description (INR/month for internships, INR/year LPA for jobs, NULL where undisclosed).  
> 5. **Future-Scope Handling:**  
   * Keep mentor\_bookings and ocr\_jobs strictly empty (0 rows).  
   * Keep mentor\_profiles empty (0 rows)—or insert only the single is\_available \= FALSE \[FUTURE SCOPE\] Mentorship Launching Soon placeholder if the UI requires a non-empty banner—and log one SEED\_INITIALIZED row in audit\_logs.

### **Verification**

* Run a SELECT row-count query across all 42 tables in Supabase PostgreSQL and confirm 0 foreign-key or constraint errors.  
* Confirm opportunities holds all 66 active/rolling records and mentor\_bookings \+ ocr\_jobs remain at 0 rows.  
* Reload the Uplink mobile app and verify Home, Jobs, Network, Messages, Q\&A, Assessments, and Profile render the fresh records cleanly.

#### **Works cited**

> 1. 2540+ Hackathons in India 2026 – Online & Student Challenges, [https\://unstop.com/hackathons](https://unstop.com/hackathons)  
> 2. CodeWarriors India 2026 \- National Hackathon \- Unstop, [https\://unstop.com/hackathons/codewarriors-india-2026-national-hackathon-buildx-1817737](https://unstop.com/hackathons/codewarriors-india-2026-national-hackathon-buildx-1817737)  
> 3. AI Kaushal Hackathon 2026 \- Unstop, [https\://unstop.com/hackathons/ai-kaushal-hackathon-2026-indian-institute-of-information-technology-iiit-lucknow-1817798](https://unstop.com/hackathons/ai-kaushal-hackathon-2026-indian-institute-of-information-technology-iiit-lucknow-1817798)  
> 4. 30 Software Development Internships in India \- Unstop, [https\://unstop.com/internship/software-development-internship](https://unstop.com/internship/software-development-internship)  
> 5. Internships & Entry-Level Jobs in India — InternDoor, [https\://interndoor.com/](https://interndoor.com/)  
> 6. Full Stack Developer Learning Path \| Self-Paced Courses \- AccioJob, [https\://acciojob.com/self-paced](https://acciojob.com/self-paced)  
> 7. Full Stack Developer Jobs for Freshers \- AccioJob Placement Records, [https\://placement.acciojob.com/jobs/?category=OFF\_CAMPUS\&role=full-stack-developer\&location=other](https://placement.acciojob.com/jobs/?category=OFF_CAMPUS&role=full-stack-developer&location=other)  
> 8. MERN Stack Jobs \- hirist.tech, [https\://www\.hirist.tech/k/mern-stack-jobs](https://www.hirist.tech/k/mern-stack-jobs)  
> 9. Full Stack Jobs \- hirist, [https\://www\.hirist.tech/c/full-stack-jobs](https://www.hirist.tech/c/full-stack-jobs)  
> 10. Student Researcher, BS/MS, Fall 2026 — Google Careers, [https\://www\.google.com/about/careers/applications/jobs/results/132362676918461126-student-researcher-bsms-fall-2026](https://www.google.com/about/careers/applications/jobs/results/132362676918461126-student-researcher-bsms-fall-2026)  
> 11. Software Engineer, AI/ML, PhD, Early Career, 2026 \- Google 人材募集, [https\://www\.google.com/about/careers/applications/jobs/results/127602843300307654-software-engineer-aiml-phd-early-career-2026?company=Google\&company=YouTube\&employment\_type=FULL\_TIME\&hl=ja\_JP\&jlo\&q\&sort\_by=relevance\&page=98](https://www.google.com/about/careers/applications/jobs/results/127602843300307654-software-engineer-aiml-phd-early-career-2026?company=Google&company=YouTube&employment_type=FULL_TIME&hl=ja_JP&jlo&q&sort_by=relevance&page=98)  
> 12. Software Engineer III \- JPMC Candidate Experience page Careers, [https\://jpmc.fa.oraclecloud.com/hcmUI/CandidateExperience/en/sites/CX\_1001/job/210808578](https://jpmc.fa.oraclecloud.com/hcmUI/CandidateExperience/en/sites/CX_1001/job/210808578)  
> 13. Search Jobs \- JPMC Candidate Experience page Careers, [https\://jpmc.fa.oraclecloud.com/hcmUI/CandidateExperience/en/sites/CX\_1001/jobs?location=India\&locationId=300000000289360\&locationLevel=country](https://jpmc.fa.oraclecloud.com/hcmUI/CandidateExperience/en/sites/CX_1001/jobs?location=India&locationId=300000000289360&locationLevel=country)  
> 14. Engineering Intern: Testing, Automation & Development at FrontPage, [https\://www\.workatastartup.com/jobs/122543](https://www.workatastartup.com/jobs/122543)  
> 15. Free ATS Score Checker for Your Resume \- NextRaise, [https\://nextraise.ai/tools/ats-resume-checker](https://nextraise.ai/tools/ats-resume-checker)  
> 16. Jobs in India, US, UK & More: Browse 2M+ Jobs by Role \| NextRaise, [https\://nextraise.ai/browse-jobs](https://nextraise.ai/browse-jobs)  
> 17. Remote Talent Acquisition Specialist Jobs in India \- Jobgether, [https\://jobgether.com/remote-jobs/india/talent-acquisition-specialist](https://jobgether.com/remote-jobs/india/talent-acquisition-specialist)  
> 18. How to Find Remote Jobs That Match Your Experience Level, [https\://jobgether.com/blog/how-to-find-remote-jobs-that-match-your-experience](https://jobgether.com/blog/how-to-find-remote-jobs-that-match-your-experience)  
> 19. India Software Engineer Jobs (NOW HIRING) \- ZipRecruiter, [https\://www\.ziprecruiter.com/Jobs/India-Software-Engineer](https://www.ziprecruiter.com/Jobs/India-Software-Engineer)  
> 20. Software Developers India Jobs (NOW HIRING) \- ZipRecruiter, [https\://www\.ziprecruiter.com/Jobs/Software-Developers-India](https://www.ziprecruiter.com/Jobs/Software-Developers-India)  
> 21. skillmeet.ai, [https\://skillmeet.ai/](https://skillmeet.ai/)  
> 22. Log In \- Wonderin \| Job Seeker AI Suite to get hired faster, [https\://wonderin.ai/resume-ai](https://wonderin.ai/resume-ai)  
> 23. Browse and Discover Jobs \| HiringCafe, [https\://hiringcafe.com/jobs](https://hiringcafe.com/jobs)  
> 24. Best Senior Software Engineer Jobs in Pittsburgh, PA 2026 \| Built In, [https\://builtin.com/jobs/pittsburgh/dev-engineering/senior](https://builtin.com/jobs/pittsburgh/dev-engineering/senior)  
> 25. Best Software Engineer Jobs in Glasgow 2026 \| Built In, [https\://builtin.com/jobs/eu/scotland/glasgow/dev-engineering/search/software-engineer](https://builtin.com/jobs/eu/scotland/glasgow/dev-engineering/search/software-engineer)  
> 26. Best Entry Level Software Engineer Jobs in Tallahassee, FL 2026, [https\://builtin.com/jobs/tallahassee/dev-engineering/entry-level](https://builtin.com/jobs/tallahassee/dev-engineering/entry-level)  
> 27. DevConnect: a home for people who build with AI, [https\://devconnectplatform.com/](https://devconnectplatform.com/)  
> 28. 33 Software Development Internships in India \- Unstop, [https\://unstop.com/internship/software-development-internships?roles=software-development\&oppstatus=open](https://unstop.com/internship/software-development-internships?roles=software-development&oppstatus=open)  
> 29. Software Engineer Intern (Summer 2027\) \- Work at a Startup, [https\://www\.workatastartup.com/jobs/110379](https://www.workatastartup.com/jobs/110379)  
> 30. Best Software Engineer Jobs in Kuala Lumpur 2026 \| Built In, [https\://builtin.com/jobs/as/malaysia/kuala-lumpur/dev-engineering/search/software-engineer](https://builtin.com/jobs/as/malaysia/kuala-lumpur/dev-engineering/search/software-engineer)  
> 31. 13th Protocol: An AI Hackathon 2026 \- Unstop, [https\://unstop.com/hackathons/13th-protocol-an-ai-hackathon-2026-voyager-coding-club-1794890](https://unstop.com/hackathons/13th-protocol-an-ai-hackathon-2026-voyager-coding-club-1794890)  
> 32. CodeTitans India 2026 \- National Hackathon, [https\://unstop.com/hackathons/codetitans-india-2026-national-hackathon-wecodecoders-1766190](https://unstop.com/hackathons/codetitans-india-2026-national-hackathon-wecodecoders-1766190)  
> 33. CodeHack India \- National Level Hackathon \- 2026 \- Unstop, [https\://unstop.com/hackathons/codehack-india-national-level-hackathon-wecodecoders-1762158](https://unstop.com/hackathons/codehack-india-national-level-hackathon-wecodecoders-1762158)  
> 34. Shakti Hackathon \- 2026 \- Unstop, [https\://unstop.com/hackathons/shakti-hackathon-bubblesort-1758572](https://unstop.com/hackathons/shakti-hackathon-bubblesort-1758572)  
> 35. 22 In Office Software Development Internships in India \- Unstop, [https\://unstop.com/internship/in-office-software-development-internship](https://unstop.com/internship/in-office-software-development-internship)  
> 36. 30 Work from Home Software Development Internships in India, [https\://unstop.com/internship/work-from-home-software-development-internships](https://unstop.com/internship/work-from-home-software-development-internships)  
> 37. 17 Work from Home Software Development Internships in India, [https\://unstop.com/internship/work-from-home-software-development-internship](https://unstop.com/internship/work-from-home-software-development-internship)  
> 38. HackNowa Global Hackathon 2026 \- Unstop, [https\://unstop.com/hackathons/hacknowa-global-hackathon-2026-innovation-hacks-1752198](https://unstop.com/hackathons/hacknowa-global-hackathon-2026-innovation-hacks-1752198)  
> 39. CodeUnited India \- National Hackathon \- 2026 \- Unstop, [https\://unstop.com/hackathons/codeunited-india-national-hackathon-hackwithpune-1817749](https://unstop.com/hackathons/codeunited-india-national-hackathon-hackwithpune-1817749)  
> 40. INNOVATE \- Hackathon 2026, [https\://unstop.com/hackathons/innovate-hackathon-2026-devharbour-1792025](https://unstop.com/hackathons/innovate-hackathon-2026-devharbour-1792025)  
> 41. 51 Software Development Internships in India \- Unstop, [https\://unstop.com/internship/software-development-internships](https://unstop.com/internship/software-development-internships)  
> 42. MERN Stack Jobs \- Hirist, [https\://www\.hirist.tech/k/mern-stack-jobs?page=5\&ref=popular\_tag](https://www.hirist.tech/k/mern-stack-jobs?page=5&ref=popular_tag)