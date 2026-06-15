import os
import glob
import xml.etree.ElementTree as ET
import pandas as pd

def parse_junit_xml(file_path, test_type):
    parsed_results = []
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        
        # In JUnit XML, the root might be <testsuite> or <testsuites>
        if root.tag == 'testsuites':
            suites = root.findall('testsuite')
        else:
            suites = [root]
            
        for suite in suites:
            suite_name = suite.attrib.get('name', 'Unknown Suite')
            for testcase in suite.findall('testcase'):
                class_name = testcase.attrib.get('classname', '')
                test_name = testcase.attrib.get('name', '')
                duration = testcase.attrib.get('time', '0')
                
                # Determine status
                status = 'PASS'
                message = ''
                stacktrace = ''
                
                failure = testcase.find('failure')
                if failure is not None:
                    status = 'FAIL'
                    message = failure.attrib.get('message', '')
                    stacktrace = failure.text or ''
                else:
                    error = testcase.find('error')
                    if error is not None:
                        status = 'ERROR'
                        message = error.attrib.get('message', '')
                        stacktrace = error.text or ''
                    else:
                        skipped = testcase.find('skipped')
                        if skipped is not None:
                            status = 'SKIPPED'
                            
                parsed_results.append({
                    "Test Type": test_type,
                    "Suite Name": suite_name,
                    "Class Name": class_name,
                    "Test Name": test_name,
                    "Duration (s)": float(duration) if duration else 0.0,
                    "Status": status,
                    "Error Message": message,
                    "Stacktrace": stacktrace.strip()
                })
    except Exception as e:
        print(f"Error parsing XML file {file_path}: {e}")
        
    return parsed_results

def main():
    workspace_dir = os.path.dirname(os.path.abspath(__file__))
    results = []
    
    # Path mappings for test results
    paths = {
        "Unit Test": os.path.join(workspace_dir, "app", "build", "test-results", "testDebugUnitTest"),
        "Instrumented E2E Test": os.path.join(workspace_dir, "app", "build", "outputs", "androidTest-results", "connected")
    }
    
    found_any = False
    for test_type, folder in paths.items():
        if os.path.exists(folder):
            # Recursively find all XML files
            xml_files = glob.glob(os.path.join(folder, "**", "*.xml"), recursive=True)
            for xml_file in xml_files:
                found_any = True
                print(f"Parsing {test_type} file: {xml_file}")
                results.extend(parse_junit_xml(xml_file, test_type))
                
    report_path = os.path.join(workspace_dir, "android_test_report.xlsx")
    
    if not found_any or not results:
        print("No test results XML files found. Generating an empty report.")
        # Create an empty report with a summary warning
        df = pd.DataFrame([{
            "Test Type": "N/A",
            "Suite Name": "N/A",
            "Class Name": "N/A",
            "Test Name": "N/A",
            "Duration (s)": 0.0,
            "Status": "NO_TESTS_FOUND",
            "Error Message": "No XML test results were found in app/build. Verify that tests compiled and executed correctly.",
            "Stacktrace": ""
        }])
    else:
        df = pd.DataFrame(results)
        
    try:
        # Sort values for readability: Test Type, Suite Name, Class Name, Test Name
        if "Suite Name" in df.columns and len(results) > 0:
            df = df.sort_values(by=["Test Type", "Suite Name", "Class Name", "Test Name"], ascending=True)
        df.to_excel(report_path, index=False)
        print(f"Excel report successfully saved to: {report_path}")
    except Exception as e:
        print(f"Failed to generate Excel report: {e}")

if __name__ == "__main__":
    main()
