const fs = require('fs');
try {
    console.log('/root contents:', fs.readdirSync('/root'));
    if (fs.existsSync('/root/.ssh')) {
        console.log('/root/.ssh contents:', fs.readdirSync('/root/.ssh'));
    } else {
        console.log('/root/.ssh does not exist');
    }
} catch (e) {
    console.log('Error:', e.message);
}
