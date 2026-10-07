import concurrent.futures
import json
import re
import ssl
import sys
import urllib.parse
import urllib.request
import urllib.error
from collections import Counter, defaultdict

# List of servers directly defined within the script
SERVERS = [
    {"name": "Voice of the Ocean", "short_name": "VOTO", "url": "https://erddap.observations.voiceoftheocean.org/erddap/", "public": True},
    {"name": "St. Lawrence Global Observatory - CIOOS | Observatoire global du Saint-Laurent - SIOOC", "short_name": "SLGO-OGSL", "url": "https://erddap.ogsl.ca/erddap/", "public": True},
    {"name": "CoastWatch West Coast Node", "short_name": "CSWC", "url": "https://coastwatch.pfeg.noaa.gov/erddap/", "public": True},
    {"name": "ERDDAP at the Asia-Pacific Data-Research Center", "short_name": "APDRC", "url": "https://apdrc.soest.hawaii.edu/erddap/", "public": True},
    {"name": "NOAA's National Centers for Environmental Information (NCEI)", "short_name": "NCEI", "url": "https://www.ncei.noaa.gov/erddap/", "public": True},
    {"name": "Biological and Chemical Oceanography Data Management Office (BCO-DMO) ERDDAP", "short_name": "BCODMO", "url": "https://erddap.bco-dmo.org/erddap/", "public": True},
    {"name": "European Marine Observation and Data Network (EMODnet) ERDDAP", "short_name": "EMODnet", "url": "https://erddap.emodnet.eu/erddap/", "public": True},
    {"name": "European Marine Observation and Data Network (EMODnet) Physics ERDDAP", "short_name": "EMODnet Physics", "url": "https://erddap.emodnet-physics.eu/erddap/", "public": True},
    {"name": "Marine Institute - Ireland", "short_name": "MII", "url": "https://erddap.marine.ie/erddap/", "public": True},
    {"name": "CoastWatch Caribbean/Gulf of Mexico Node", "short_name": "CSCGOM", "url": "https://cwcgom.aoml.noaa.gov/erddap/", "public": True},
    {"name": "NOAA IOOS Sensors ERDDAP", "short_name": "IOOS-Sensors", "url": "https://erddap.sensors.ioos.us/erddap/", "public": True},
    {"name": "CeNCOOS (Central and Northern California Ocean Observing System) ERDDAP", "short_name": "CeNCOOS ERDDAP", "url": "http://erddap.cencoos.org/erddap/", "public": True},
    {"name": "NOAA IOOS NERACOOS (Northeastern Regional Association of Coastal and Ocean Observing Systems)", "short_name": "NERACOOS", "url": "https://data.neracoos.org/erddap/", "public": True},
    {"name": "NOAA IOOS NGDAC (National Glider Data Assembly Center)", "short_name": "NGDAC", "url": "https://gliders.ioos.us/erddap/", "public": True},
    {"name": "NOAA IOOS PacIOOS (Pacific Islands Ocean Observing System) at the University of Hawaii (UH)", "short_name": "PacIOOS", "url": "https://pae-paha.pacioos.hawaii.edu/erddap/", "public": True},
    {"name": "Southern California Coastal Ocean Observing System (SCCOOS)", "short_name": "SCCOOS", "url": "https://sccoos.org/erddap/", "public": True},
    {"name": "NOAA IOOS SECOORA (Southeast Coastal Ocean Observing Regional Association)", "short_name": "SECOORA", "url": "http://erddap.secoora.org/erddap/", "public": True},
    {"name": "NOAA OSMC (Observing System Monitoring Center)", "short_name": "OSMC", "url": "http://osmc.noaa.gov/erddap/", "public": True},
    {"name": "ONC (Ocean Networks Canada)", "short_name": "ONC", "url": "http://dap.onc.uvic.ca/erddap/", "public": True},
    {"name": "OTN (Ocean Tracking Network)", "short_name": "OTN", "url": "http://erddap.oceantrack.org/erddap/", "public": True},
    {"name": "NOAA CoastWatch Hawaii-Pacific", "short_name": "PIFSC", "url": "https://oceanwatch.pifsc.noaa.gov/erddap/", "public": False},
    {"name": "Ocean Observatories Initiative (OOI)", "short_name": "OOI", "url": "https://erddap.dataexplorer.oceanobservatories.org/erddap/", "public": True},
    {"name": "Ocean Observatories Initiative (OOI) Goldcopy", "short_name": "OOI Goldcopy", "url": "https://erddap-goldcopy.dataexplorer.oceanobservatories.org/erddap/", "public": True},
    {"name": "Regional Ocean Modelling System", "short_name": "MYROMS", "url": "http://www.myroms.org:8080/erddap/", "public": False},
    {"name": "Department of Marine and Coastal Sciences, School of Environmental and Biological Sciences, Rutgers, The State University of New Jersey", "short_name": "RUTGERS", "url": "http://tds.marine.rutgers.edu/erddap/", "public": True},
    {"name": "NOAA NEFSC", "short_name": "NEFSC", "url": "https://comet.nefsc.noaa.gov/erddap/", "public": False},
    {"name": "NOAA's Center for Operational Oceanographic Products and Services", "short_name": "COOPS-NOS", "url": "https://opendap.co-ops.nos.noaa.gov/erddap/", "public": True},
    {"name": "GCOOS Atmospheric and Oceanographic: Historical Data", "short_name": "GCOO5-TAMU", "url": "https://gcoos5.geos.tamu.edu/erddap/", "public": False},
    {"name": "GCOOS Biological and Socioeconomics", "short_name": "GCOO4-TAMU", "url": "https://gcoos4.tamu.edu/erddap/", "public": False},
    {"name": "NOAA CoastWatch Great Lakes Node", "short_name": "GLERL", "url": "https://apps.glerl.noaa.gov/erddap/", "public": True},
    {"name": "Spray Underwater Glider data from Instrument Development Group, Scripps Institute of Oceanography, University of California, San Diego", "short_name": "UCSD", "url": "https://spraydata.ucsd.edu/erddap/", "public": True},
    {"name": "UBC Earth, Ocean & Atmospheric Sciences SalishSeaCast Project", "short_name": "UBC", "url": "https://salishsea.eos.ubc.ca/erddap/", "public": True},
    {"name": "UC Davis BML (University of California at Davis, Bodega Marine Laboratory)", "short_name": "BMLSC", "url": "http://bmlsc.ucdavis.edu:8080/erddap/", "public": True},
    {"name": "NOAA UAF (Unified Access Framework)", "short_name": "UAF", "url": "https://upwell.pfeg.noaa.gov/erddap/", "public": True},
    {"name": "French Research Institute for the Exploitation of the Sea", "short_name": "IFREMER", "url": "https://www.ifremer.fr/erddap/", "public": True},
    {"name": "NOAA PMEL (Pacific Marine Environmental Laboratory)", "short_name": "PMEL", "url": "https://data.pmel.noaa.gov/pmel/erddap/", "public": True},
    {"name": "ALAMO (Air Launched Autonomous Micro-Observer)", "short_name": "ALAMO", "url": "https://ferret.pmel.noaa.gov/alamo/erddap/", "public": True},
    {"name": "SOCAT (Surface Ocean CO2 ATlas)", "short_name": "SOCAT", "url": "https://ferret.pmel.noaa.gov/socat/erddap/", "public": True},
    {"name": "Hakai Institute", "short_name": "Hakai", "url": "https://catalogue.hakai.org/erddap/", "public": True},
    {"name": "NOAA Polar Watch", "short_name": "POLARWATCH", "url": "https://polarwatch.noaa.gov/erddap/", "public": True},
    {"name": "USGS Coastal and Marine Geology Program", "short_name": "USGS", "url": "https://geoport.usgs.esipfed.org/erddap/", "public": True},
    {"name": "ESSO INCOIS (Indian National Centre for Ocean Information Services)", "short_name": "INCOIS", "url": "https://erddap.incois.gov.in/erddap/", "public": True},
    {"name": "Smart Atlantic", "short_name": "SmartAtlantic", "url": "https://www.smartatlantic.ca/erddap/", "public": True},
    {"name": "GRIIDC (Gulf of Mexico Research Initiative", "short_name": "GRIIDC", "url": "https://erddap.griidc.org/erddap/", "public": True},
    {"name": "NOAA ATN IOOS (Animal Telemetry Network)", "short_name": "ATN-IOOS", "url": "https://atn.ioos.us/erddap/", "public": True},
    {"name": "DIVER (NOAA Office of Response and Restoration)", "short_name": "DIVER", "url": "https://pub-data.diver.orr.noaa.gov/erddap/", "public": True},
    {"name": "GCOOS Atmospheric and Oceanographic: Observing System", "short_name": "GCOOS", "url": "https://erddap.gcoos.org/erddap/", "public": True},
    {"name": "University of Delaware, College of Earth, Ocean and Environment", "short_name": "CEOE", "url": "https://basin.ceoe.udel.edu/erddap/", "public": True},
    {"name": "Canadian Integrated Ocean Observatory System Atlantic", "short_name": "CIOOS Atlantic", "url": "https://cioosatlantic.ca/erddap/", "public": True},
    {"name": "Canadian Integrated Ocean Observatory System Pacific", "short_name": "CIOOS Pacific", "url": "https://data.cioospacific.ca/erddap/", "public": True},
    {"name": "European Multidisciplinary Seafloor and water column Observatory (EMSO)", "short_name": "EMSO ERIC", "url": "http://erddap.emso.eu/erddap/", "public": True},
    {"name": "NOAA CoastWatch Central Operations", "short_name": "NCCO", "url": "https://coastwatch.noaa.gov/erddap/", "public": True},
    {"name": "The Canadian Watershed Information Network at the University of Manitoba", "short_name": "CanWIN", "url": "https://canwinerddap.ad.umanitoba.ca/erddap/", "public": True},
    {"name": "NOAA Oceanview", "short_name": "NOAA Oceanview", "url": "https://oceanview.pfeg.noaa.gov/erddap/", "public": True},
    {"name": "UNESCO IOC-IODE Ocean Acidification", "short_name": "IOC-IODE-OA", "url": "https://erddap.oa.iode.org/erddap/", "public": True},
    {"name": "Linked Systems @ the British Oceanographic Data Centre, National Oceanography Centre, UK", "short_name": "NOC-BODC Linked Systems", "url": "https://linkedsystems.uk/erddap/", "public": True},
    {"name": "Bio-Oracle Erddap Server", "short_name": "Bio-Oracle", "url": "https://erddap.bio-oracle.org/erddap/", "public": True},
    {"name": "Commercial Fisheries Research Foundation", "short_name": "CFRF", "url": "https://erddap.ondeckdata.com/erddap/", "public": True},
    # --- Added from fleet_prometheus.yml ---
    {"name": "NOAA IOOS AOOS (Alaska Ocean Observing System)", "short_name": "AOOS", "url": "https://erddap.aoos.org/erddap/", "public": True},
    {"name": "NOAA IOOS MARACOOS (Mid-Atlantic Regional Association Coastal Ocean Observing Systems)", "short_name": "MARACOOS", "url": "https://erddap.maracoos.org/erddap/", "public": True},
    {"name": "NANOOS (Northwest Association of Networked Ocean Observing Systems)", "short_name": "NANOOS", "url": "https://erddap.nanoos.org/erddap/", "public": True},
    {"name": "NOAA IOOS GLOS (Great Lakes Observing System)", "short_name": "GLOS", "url": "https://seagull-erddap.glos.org/erddap/", "public": True},
    {"name": "NSERC PermafrostNet", "short_name": "PermaFrost", "url": "https://data.permafrostnet.ca/erddap/", "public": True},
    {"name": "EMSO - European Multidisciplinary Seafloor and water column Observatory", "short_name": "EMSO", "url": "https://erddap.emso-fr.org/erddap/", "public": True},
    {"name": "Italian Arctic Data Center", "short_name": "IADC", "url": "https://data.iadc.cnr.it/erddap/", "public": True},
    {"name": "National Ocean Data Centre - OGS", "short_name": "NODC-OGS", "url": "https://nodc.ogs.it/erddap/", "public": True},
    {"name": "NOAA Global Drifter Program", "short_name": "AOML/GDP", "url": "https://erddap.aoml.noaa.gov/gdp/erddap/", "public": True},
    # --- New additions
    { "name": "NOAA IOOS GCOOS Biological and Socioeconomics", "short_name": "GCOOSBAS", "url": "https://gcoos4.geos.tamu.edu/erddap/", "public": True},
    { "name": "ERDDAP at RI Data Discovery Center", "short_name": "Brown", "url": "https://erddap.riddc.brown.edu/erddap/", "public": True},
    { "name": "National Antarctic Data Center", "short_name": "NADC", "url": "https://antarcticdatacenter.cnr.it/erddap/", "public": True},
    { "name": "Backyard Buoys", "short_name": "Backyard", "url": "https://erddap.backyardbuoys.org/erddap/", "public": True},
]
SSL_CONTEXT = ssl.create_default_context()
SSL_CONTEXT.check_hostname = False
SSL_CONTEXT.verify_mode = ssl.CERT_NONE

METRIC_LINE_RE = re.compile(
    r'^([a-zA-Z_][a-zA-Z0-9_]*)(?:\{(.*?)\})?\s+([^\s]+)(?:\s+\d+)?$'
)
LABEL_PAIR_RE = re.compile(
    r'([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*"([^"\\]*(?:\\.[^"\\]*)*)"'
)


def parse_prometheus_text(text: str) -> list[dict]:
    """Parses Prometheus exposition format plain text into structured metrics."""
    parsed_metrics = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue

        match = METRIC_LINE_RE.match(line)
        if match:
            metric_name, raw_labels, val_str = match.groups()
            labels = {}
            if raw_labels:
                for k, v in LABEL_PAIR_RE.findall(raw_labels):
                    labels[k] = v
            try:
                value = float(val_str)
            except ValueError:
                value = val_str

            parsed_metrics.append({
                'name': metric_name,
                'labels': labels,
                'value': value
            })
    return parsed_metrics


def get_endpoints() -> list[dict]:
    """Generates metric target endpoints directly from the internal SERVERS list."""
    endpoints = []
    for server in SERVERS:
        base_url = server['url'].rstrip('/')
        metrics_url = f"{base_url}/metrics"
        target = urllib.parse.urlparse(base_url).netloc
        name = server['name'] if server['name'] else server['short_name']

        endpoints.append({
            'target': target,
            'url': metrics_url,
            'base_url': server['url'],
            'name': name,
            'short_name': server['short_name'],
            'public': server.get('public', True)
        })
    return endpoints


def fetch_server_snapshot(endpoint: dict, timeout: int = 8) -> dict:
    """Fetches metrics for a single endpoint and extracts build info and feature flags."""
    url = endpoint['url']
    req = urllib.request.Request(
        url,
        headers={'User-Agent': 'ERDDAP-Fleet-Checker/1.0'}
    )

    result = {
        'target': endpoint['target'],
        'short_name': endpoint['short_name'],
        'name': endpoint['name'],
        'url': url,
        'status': 'DOWN',
        'erddap_version': 'Unknown',
        'version_full': 'Unknown',
        'deployment_info': 'Unknown',
        'feature_flags': {},
        'error': None
    }

    try:
        with urllib.request.urlopen(req, timeout=timeout, context=SSL_CONTEXT) as resp:
            if resp.status == 200:
                body = resp.read().decode('utf-8', errors='replace')
                metrics = parse_prometheus_text(body)
                result['status'] = 'UP'

                for m in metrics:
                    if m['name'] == 'ERDDAP_build_info':
                        labels = m['labels']
                        result['erddap_version'] = labels.get('version', 'Unknown')
                        result['version_full'] = labels.get('version_full', 'Unknown')
                        result['deployment_info'] = labels.get('deployment_info', 'Unknown')

                    elif m['name'] == 'feature_flags':
                        labels = m['labels']
                        flag_name = labels.get('feature_flags')
                        if flag_name:
                            val = m['value']
                            if isinstance(val, (int, float)):
                                state = "enabled" if val == 1.0 else "disabled"
                            else:
                                state = str(val)
                            result['feature_flags'][flag_name] = state

    except urllib.error.HTTPError as e:
        result['error'] = f"HTTP {e.code}"
    except urllib.error.URLError as e:
        result['error'] = f"URL Error: {e.reason}"
    except Exception as e:
        result['error'] = str(e)

    return result


def generate_report(results: list[dict]):
    """Prints a summarized breakdown of deployed versions and feature flags."""
    total_servers = len(results)
    up_servers = [r for r in results if r['status'] == 'UP']
    down_servers = [r for r in results if r['status'] == 'DOWN']

    version_counts = Counter(r['version_full'] for r in up_servers)

    feature_flag_summary = defaultdict(Counter)
    for r in up_servers:
        for flag, state in r['feature_flags'].items():
            feature_flag_summary[flag][state] += 1

    print("=" * 80)
    print(" FLEET METRICS SNAPSHOT REPORT")
    print(f" Total Endpoints: {total_servers} | UP: {len(up_servers)} | DOWN/Unreachable: {len(down_servers)}")
    print("=" * 80)

    print("\n--- Deployed ERDDAP Version Distribution ---")
    for ver, count in version_counts.most_common():
        pct = (count / len(up_servers) * 100) if up_servers else 0
        print(f"  • {ver:<35} : {count:2d} servers ({pct:.1f}%)")

    print("\n--- Feature Flags Summary across Fleet ---")
    if feature_flag_summary:
        for flag in sorted(feature_flag_summary.keys()):
            states = feature_flag_summary[flag]
            states_str = ", ".join(f"{state}: {cnt}" for state, cnt in sorted(states.items()))
            print(f"  • {flag:<35} -> {states_str}")
    else:
        print("  (No feature_flags metrics exported or parsed)")

    print("\n--- Unreachable / Down Servers ---")
    if down_servers:
        for r in down_servers:
            print(f"  • [{r['short_name']}] {r['target']} ({r['url']}) - Error: {r['error']}")
    else:
        print("  None (All servers responded successfully)")

    print("\n" + "=" * 80)


def main():
    endpoints = get_endpoints()
    print(f"Scraping metrics from {len(endpoints)} servers concurrently...")

    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
        future_to_endpoint = {executor.submit(fetch_server_snapshot, ep): ep for ep in endpoints}
        for future in concurrent.futures.as_completed(future_to_endpoint):
            results.append(future.result())

    generate_report(results)

    output_filename = 'erddap_fleet_snapshot.json'
    with open(output_filename, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2)
    print(f"Full snapshot details saved to '{output_filename}'.")


if __name__ == '__main__':
    main()