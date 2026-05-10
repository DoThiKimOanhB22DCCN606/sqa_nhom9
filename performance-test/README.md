# Performance Testing

This directory contains JMeter performance test scripts for the recruitment system.

## 📁 Structure

```
performance-test/
├── jmeter/
│   ├── scripts/
│   │   └── PT-LOGIN_Performance.jmx    # Login performance test script
│   └── csv/
│       └── login_accounts.csv          # Test data for login accounts
```

## 🚀 How to Run

### Prerequisites
- Apache JMeter installed (version 5.5+ recommended)
- Java 8 or higher

### Running the Tests

1. **Open JMeter GUI:**
   ```bash
   jmeter
   ```

2. **Load the test script:**
   - File → Open → Select `PT-LOGIN_Performance.jmx`

3. **Configure test parameters:**
   - Update CSV file path in CSV Data Set Config if needed
   - Adjust Thread Group settings (users, ramp-up time, etc.)

4. **Run the test:**
   - Run → Start (Ctrl+R)

### Command Line Execution

```bash
jmeter -n -t "performance-test/jmeter/scripts/PT-LOGIN_Performance.jmx" -l results.jtl
```

## 📊 Test Scenarios

### PT-LOGIN Performance Test
- **Purpose:** Test login functionality under load
- **Test Data:** `login_accounts.csv` contains user credentials
- **Configuration:**
  - Number of Threads: 10 (configurable)
  - Ramp-up Period: 30 seconds
  - Loop Count: 5

## 📈 Results Analysis

After running the test, analyze the results using:
- JMeter's built-in listeners (View Results Tree, Summary Report)
- Generate HTML reports with:
  ```bash
  jmeter -g results.jtl -o report/
  ```

## 🔧 Configuration

### CSV Data Set Config
- **Filename:** `login_accounts.csv`
- **Variable Names:** username,password
- **Delimiter:** Comma (,)

### HTTP Request Defaults
- **Server Name:** localhost (update for your environment)
- **Port Number:** 8080 (update for your environment)
- **Protocol:** http

## 📝 Notes

- Update server configuration in HTTP Request Defaults before running
- Ensure the application is running before starting performance tests
- Monitor system resources during test execution
- Adjust thread counts based on your system's capacity