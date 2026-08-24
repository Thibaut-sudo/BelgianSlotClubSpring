(function (global) {
    function byId(id) {
        return document.getElementById(id);
    }

    function promptImportPassword() {
        return new Promise(function (resolve) {
            var modal = byId('importPasswordModal');
            var form = byId('importPasswordForm');
            var input = byId('importPasswordInput');
            var errorEl = byId('importPasswordError');
            var cancelBtn = byId('importPasswordCancel');
            var submitBtn = byId('importPasswordSubmit');

            if (!modal || !form || !input) {
                resolve(null);
                return;
            }

            var settled = false;

            function finish(result) {
                if (settled) return;
                settled = true;
                modal.style.display = 'none';
                form.removeEventListener('submit', onSubmit);
                if (cancelBtn) cancelBtn.removeEventListener('click', onCancel);
                modal.removeEventListener('click', onBackdrop);
                document.removeEventListener('keydown', onKey);
                if (submitBtn) submitBtn.disabled = false;
                resolve(result);
            }

            function onCancel(e) {
                if (e) e.preventDefault();
                finish(null);
            }

            function onBackdrop(e) {
                if (e.target === modal) finish(null);
            }

            function onKey(e) {
                if (e.key === 'Escape') finish(null);
            }

            async function onSubmit(e) {
                e.preventDefault();
                var password = input.value;
                if (errorEl) {
                    errorEl.hidden = true;
                    errorEl.textContent = '';
                }
                if (!password) {
                    if (errorEl) {
                        errorEl.textContent = 'Saisissez le mot de passe.';
                        errorEl.hidden = false;
                    }
                    input.focus();
                    return;
                }
                if (submitBtn) submitBtn.disabled = true;
                try {
                    var res = await fetch('/api/verify-password', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ password: password })
                    });
                    if (res.ok) {
                        finish(password);
                        return;
                    }
                    if (errorEl) {
                        errorEl.textContent = 'Mot de passe incorrect.';
                        errorEl.hidden = false;
                    }
                    input.select();
                } catch (err) {
                    if (errorEl) {
                        errorEl.textContent = 'Impossible de vérifier le mot de passe.';
                        errorEl.hidden = false;
                    }
                }
                if (submitBtn) submitBtn.disabled = false;
            }

            input.value = '';
            if (errorEl) {
                errorEl.hidden = true;
                errorEl.textContent = '';
            }
            modal.style.display = 'flex';
            form.addEventListener('submit', onSubmit);
            if (cancelBtn) cancelBtn.addEventListener('click', onCancel);
            modal.addEventListener('click', onBackdrop);
            document.addEventListener('keydown', onKey);
            setTimeout(function () { input.focus(); }, 50);
        });
    }

    function setFormPassword(form, password) {
        if (!form) return;
        var hidden = form.querySelector('input[name="password"]');
        if (!hidden) {
            hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.name = 'password';
            form.appendChild(hidden);
        }
        hidden.value = password;
    }

    function protectImportForm(form) {
        if (!form || form.dataset.importProtected === '1') return;
        form.dataset.importProtected = '1';
        form.addEventListener('submit', async function (e) {
            if (form.dataset.importUnlocked === '1') return;
            e.preventDefault();
            var password = await promptImportPassword();
            if (!password) return;
            setFormPassword(form, password);
            form.dataset.importUnlocked = '1';
            if (typeof form.requestSubmit === 'function') {
                form.requestSubmit();
            } else {
                form.submit();
            }
        });
    }

    global.promptImportPassword = promptImportPassword;
    global.setImportFormPassword = setFormPassword;
    global.protectImportForm = protectImportForm;
})(window);
