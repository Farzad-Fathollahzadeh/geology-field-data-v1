const { execSync } = require('child_process');
try {
    const version = execSync('git --version', { encoding: 'utf8' });
    console.log('Git version:', version.trim());
    try {
        const status = execSync('git status', { encoding: 'utf8' });
        console.log('Git status:', status);
    } catch (e) {
        console.log('Git status failed:', e.message);
    }
} catch (e) {
    console.error('Error:', e.message);
}
