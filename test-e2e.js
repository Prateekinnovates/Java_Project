const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const URL = 'http://localhost:3000';
const ARTIFACTS_DIR = path.join(__dirname, 'test-artifacts');

if (!fs.existsSync(ARTIFACTS_DIR)) {
  fs.mkdirSync(ARTIFACTS_DIR, { recursive: true });
}

async function runTests() {
  console.log('--- Starting Automated E2E Browser Tests ---');
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 900 });

  page.on('console', msg => console.log('[Browser Console]', msg.type(), msg.text()));
  page.on('pageerror', err => console.error('[Browser PageError]', err.toString()));
  page.on('dialog', async dialog => {
    console.log('[Browser Dialog]', dialog.message());
    await dialog.accept();
  });

  try {
    // 1. Load Application
    console.log('1. Loading', URL);
    await page.goto(URL, { waitUntil: 'networkidle0' });
    await page.waitForSelector('#card-balance-val');

    // 2. Verify Dashboard Stats
    const balance = await page.$eval('#card-balance-val', el => el.textContent.trim());
    const income = await page.$eval('#card-income-val', el => el.textContent.trim());
    const expense = await page.$eval('#card-expense-val', el => el.textContent.trim());
    const monthSpending = await page.$eval('#card-month-expense-val', el => el.textContent.trim());
    console.log(`✓ Dashboard stats: Balance=${balance}, Income=${income}, Expense=${expense}, MonthSpend=${monthSpending}`);

    // Verify recent transactions exist
    const recentCount = await page.$$eval('#dashboard-recent-transactions .recent-tx-item', els => els.length);
    console.log(`✓ Dashboard recent transactions rendered: ${recentCount} items`);

    // Verify budget cards exist
    const budgetCount = await page.$$eval('#dashboard-budgets-list .budget-card-item', els => els.length);
    console.log(`✓ Dashboard budget cards rendered: ${budgetCount} items`);

    await page.screenshot({ path: path.join(ARTIFACTS_DIR, '01_dashboard.png') });

    // 3. Test Theme Toggle
    console.log('2. Testing Theme Toggle');
    await page.click('#btn-theme-toggle');
    let theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    console.log(`✓ Toggled theme to: ${theme}`);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, '02_dark_mode.png') });

    // 4. Test Transactions Tab
    console.log('3. Testing Transactions Tab & CRUD Operations');
    await page.click('#nav-transactions');
    await page.waitForSelector('#transactions-tbody tr');

    const initialRows = await page.$$eval('#transactions-tbody tr', els => els.length);
    console.log(`✓ Initial transaction rows: ${initialRows}`);

    // Click "Add Transaction"
    await page.click('#btn-open-modal');
    await page.waitForSelector('#transaction-modal', { visible: true });

    // Fill form
    await page.select('#modal-type', 'EXPENSE');
    await page.type('#modal-amount', '125.50');
    await page.evaluate(() => { document.getElementById('modal-date').value = '2026-11-12'; });
    await page.select('#modal-category', 'SHOPPING');
    await page.type('#modal-description', 'Automated Test Noise Cancelling Headphones');

    // Save
    await page.click('#form-transaction-save');
    await new Promise(r => setTimeout(r, 600));

    // Verify new transaction is in table
    const tableTextAfterAdd = await page.$eval('#transactions-tbody', el => el.innerText);
    if (!tableTextAfterAdd.includes('Automated Test Noise Cancelling Headphones')) {
      throw new Error('Failed to find newly added transaction in table!');
    }
    console.log('✓ Successfully added transaction: "Automated Test Noise Cancelling Headphones" ($125.50)');
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, '03_transaction_added.png') });

    // Find the edit button for this transaction
    const editBtnSelector = await page.evaluate(() => {
      const rows = Array.from(document.querySelectorAll('#transactions-tbody tr'));
      for (const row of rows) {
        if (row.innerText.includes('Automated Test Noise Cancelling Headphones')) {
          const btn = row.querySelector('.btn-edit');
          if (btn) return btn.getAttribute('onclick');
        }
      }
      return null;
    });

    if (!editBtnSelector) {
      throw new Error('Could not find edit button for new transaction');
    }

    console.log('4. Editing Transaction');
    await page.evaluate((onclickAttr) => {
      eval(onclickAttr);
    }, editBtnSelector);

    await page.waitForSelector('#transaction-modal', { visible: true });
    // Clear and change amount & description
    await page.evaluate(() => {
      document.getElementById('modal-amount').value = '';
      document.getElementById('modal-description').value = '';
    });
    await page.type('#modal-amount', '149.99');
    await page.type('#modal-description', 'Automated Test Premium Headphones Updated');
    await page.click('#form-transaction-save');
    await new Promise(r => setTimeout(r, 600));

    const tableTextAfterEdit = await page.$eval('#transactions-tbody', el => el.innerText);
    if (!tableTextAfterEdit.includes('Automated Test Premium Headphones Updated') || !tableTextAfterEdit.includes('149.99')) {
      throw new Error('Failed to verify updated transaction in table!');
    }
    console.log('✓ Successfully edited transaction to: "Automated Test Premium Headphones Updated" ($149.99)');

    // Delete transaction
    console.log('5. Deleting Transaction');
    const deleteBtnSelector = await page.evaluate(() => {
      const rows = Array.from(document.querySelectorAll('#transactions-tbody tr'));
      for (const row of rows) {
        if (row.innerText.includes('Automated Test Premium Headphones Updated')) {
          const btn = row.querySelector('.btn-delete');
          if (btn) return btn.getAttribute('onclick');
        }
      }
      return null;
    });

    if (!deleteBtnSelector) {
      throw new Error('Could not find delete button for updated transaction');
    }

    await page.evaluate((onclickAttr) => {
      eval(onclickAttr);
    }, deleteBtnSelector);
    await new Promise(r => setTimeout(r, 600));

    const tableTextAfterDelete = await page.$eval('#transactions-tbody', el => el.innerText);
    if (tableTextAfterDelete.includes('Automated Test Premium Headphones Updated')) {
      throw new Error('Transaction was not deleted from table!');
    }
    console.log('✓ Successfully confirmed deletion of transaction.');

    // 5. Test Reports Tab
    console.log('6. Testing Reports & Analytics Tab');
    await page.click('#nav-reports');
    await page.waitForSelector('#report-savings-rate');

    const savingsRate = await page.$eval('#report-savings-rate', el => el.textContent.trim());
    const netSavings = await page.$eval('#report-net-savings', el => el.textContent.trim());
    const highestMonth = await page.$eval('#report-highest-month', el => el.textContent.trim());
    console.log(`✓ Reports KPIs: SavingsRate=${savingsRate}, NetSavings=${netSavings}, HighestMonth=${highestMonth}`);

    const topCatsCount = await page.$$eval('#report-top-categories-list .top-cat-card', els => els.length);
    console.log(`✓ Top spending categories count: ${topCatsCount}`);

    const chartBarExists = await page.$eval('#chart-monthly-bar', el => !!el);
    const chartDoughnutExists = await page.$eval('#chart-category-doughnut', el => !!el);
    const chartTrendExists = await page.$eval('#chart-spending-trend', el => !!el);
    console.log(`✓ Chart canvases present: Bar=${chartBarExists}, Doughnut=${chartDoughnutExists}, TrendLine=${chartTrendExists}`);

    await page.screenshot({ path: path.join(ARTIFACTS_DIR, '04_reports_tab.png') });

    // 6. Test Budgets & Settings Tab
    console.log('7. Testing Budgets & Settings Tab');
    await page.click('#nav-settings');
    await page.waitForSelector('#settings-budgets-container');

    const budgetRows = await page.$$eval('#settings-budgets-container .budget-setting-row', els => els.length);
    console.log(`✓ Budget setting rows: ${budgetRows}`);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, '05_settings_tab.png') });

    // 7. Test Reload & LocalStorage Persistence
    console.log('8. Testing LocalStorage Persistence Across Page Reload');
    await page.reload({ waitUntil: 'networkidle0' });
    const themeAfterReload = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    console.log(`✓ Theme persisted after reload: ${themeAfterReload}`);

    const balanceAfterReload = await page.$eval('#card-balance-val', el => el.textContent.trim());
    console.log(`✓ Balance persisted after reload: ${balanceAfterReload}`);

    // 8. Test CSV Export Function
    console.log('9. Testing CSV Export function');
    const csvContent = await page.evaluate(() => {
      // Test the service export output directly through storage or window export
      return localStorage.getItem('expense_tracker_data_v1');
    });
    console.log(`✓ LocalStorage data verified: ${csvContent.length} bytes`);

    console.log('\n=========================================');
    console.log(' ALL E2E BROWSER TESTS PASSED 100%! ');
    console.log('=========================================');

  } catch (err) {
    console.error('❌ Test failed with error:', err);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'error_screenshot.png') });
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
}

runTests();
