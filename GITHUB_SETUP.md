# Publish FreshMart to your GitHub account

No repository was created by this delivery. An email address alone does not provide access to an account. Use the GitHub account associated with **hamdhanm30@gmail.com** and obtain its actual username from your profile.

1. Sign in to GitHub in your browser. Create a repository called `freshmart-cw2`. Choose visibility appropriate for your coursework; a private repository requires inviting the marker, while a public repository should contain only code and fictional evidence. Leave the new repository empty (no initial README, licence or .gitignore).
2. Extract the ZIP and open the **FreshMart** folder in VS Code. Run the tests first. The provided `.gitignore` excludes local data, invoices and compiled files.
3. Open the terminal in that folder. Replace `YOUR_USERNAME` below with your verified GitHub username. These commands initialise a new local repository; use them only in this extracted project folder.

```bash
git init
git config user.name "Hamdhan Mohamed Ameen"
git config user.email "hamdhanm30@gmail.com"
git add .
git status
git commit -m "Implement FreshMart coursework with login and tests"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/freshmart-cw2.git
git push -u origin main
```

4. Complete GitHub's browser authentication when Git Credential Manager prompts. Never paste a password or access token into the report or source files. If Git is not installed, install it before using these commands.
5. Open the repository URL, verify that `src`, `test`, `docs`, the scripts and README are present and data files are absent. Confirm marker access. Copy that real URL into **Section 5 Project Folder Link** in the editable Word report, then export an updated PDF.
6. Submit the PDF and compressed project as required by ELMS. The supplied brief contains inconsistent group wording, although its title and expected work explicitly say individual; follow the latest lecturer/ELMS submission instructions.

If an `origin` already exists, inspect `git remote -v` before changing anything. Do not force-push into an unrelated repository. GitHub Pages is unnecessary: this Java CLI runs on your computer, while the repository stores and shares its source.

Source: GitHub (n.d.) *Adding locally hosted code to GitHub*. Available at: https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github (Accessed: 10 November 2026).
