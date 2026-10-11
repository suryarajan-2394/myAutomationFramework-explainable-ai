pipeline {
  agent any

  tools {
    maven 'MavenLocal'    // adjust to the name of your Maven tool in Jenkins global config
  }

  options { timestamps(); disableConcurrentBuilds(); skipDefaultCheckout(true) }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Prepare current-run evidence') {
      steps {
        // Delete generated outputs only; archived evidence from previous builds remains in Jenkins.
        dir('AutomationReports') { deleteDir() }
        dir('target/surefire-reports') { deleteDir() }
        script {
          if (isUnix()) { sh 'rm -f AutomationReports.zip' }
          else { bat 'if exist AutomationReports.zip del /F /Q AutomationReports.zip' }
        }
      }
    }

    stage('Build & Run Tests') {
      steps {
        script {
          if (isUnix()) {
            sh 'mvn -B -Pstakeholder-report clean test'
          } else {
            bat 'mvn -B -Pstakeholder-report clean test'
          }
        }
      }
    }


  }

  post {
    always {
      script {
        echo ">>> post.always: Ensure AutomationReports.zip exists and prepare attachments"

        // -- create AutomationReports.zip if missing --
        def zipName = 'AutomationReports.zip'
        if (!fileExists(zipName)) {
          echo "AutomationReports.zip not found; will try to create from AutomationReports folder (if present)."

          if (isUnix()) {
            sh '''
              if [ -d "AutomationReports" ]; then
                rm -f AutomationReports.zip || true
                (cd AutomationReports && zip -r ../AutomationReports.zip .) || true
                echo "Created AutomationReports.zip (unix)"
              else
                echo "AutomationReports folder not present; skipping zip creation (unix)"
              fi
            '''
          } else {
            // Windows: use powershell Compress-Archive safely
            bat '''
              if exist AutomationReports.zip del /F /Q AutomationReports.zip
              powershell -NoProfile -Command ^
                "$src = Join-Path $env:WORKSPACE 'AutomationReports';" ^
                "if (Test-Path $src) { Compress-Archive -Path (Join-Path $src '*') -DestinationPath AutomationReports.zip -Force; Write-Output 'Created AutomationReports.zip (windows)'} else { Write-Output 'AutomationReports folder not present - skipping zip creation (windows)'}"
            '''
          }
        } else {
          echo "AutomationReports.zip already present - will attempt to attach it."
        }

        // Archive current evidence even when Maven fails. Never publish old workspace results.
        archiveArtifacts artifacts: 'AutomationReports/**, target/surefire-reports/**, AutomationReports.zip',
                         fingerprint: true, allowEmptyArchive: true
        junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: true

        // -- ensure TestAutomationReport.html full path if it's under AutomationReports --
        def htmlPath = 'AutomationReports/TestAutomationReport.html'
        if (!fileExists(htmlPath)) {
          echo "TestAutomationReport.html not found at AutomationReports/TestAutomationReport.html — trying workspace root."
          if (fileExists('TestAutomationReport.html')) {
            htmlPath = 'TestAutomationReport.html'
            echo "Found TestAutomationReport.html at workspace root; will attach from root."
          } else {
            echo "No TestAutomationReport.html found."
            htmlPath = null
          }
        } else {
          echo "Found TestAutomationReport.html at ${htmlPath}"
        }

        // -- build attachments list only for files that exist --
        def attachments = []
        if (fileExists('AutomationReports.zip')) {
          // optionally check size > 0
          attachments << 'AutomationReports.zip'
          echo "Will attach AutomationReports.zip"
        } else {
          echo "AutomationReports.zip not available to attach"
        }
        if (htmlPath) {
          attachments << htmlPath
          echo "Will attach ${htmlPath}"
        }

        // The timestamped explanation includes the benchmark summary and repair history.
        if (fileExists('AutomationReports/explainable')) {
          attachments << 'AutomationReports/explainable/explanation-*.html'
        }

        // -- Compose email --
        def subject = "${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}"
        def body = """
          <p>Build: <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a></p>
          <p>Result: ${currentBuild.currentResult}</p>
          <p>Attached: ${attachments.size() > 0 ? attachments.join(', ') : 'None'}</p>
          <p>If attachments are blocked by Gmail, you can still download artifacts from: <a href='${env.BUILD_URL}artifact/'>Artifacts</a></p>
        """

        // -- Send email using emailext --
        if (attachments.size() > 0) {
          emailext (
            to: 'suryarajan.selvarajan@gmail.com',              // <-- change recipient
            subject: subject,
            body: body,
            mimeType: 'text/html',
            attachmentsPattern: attachments.join(','),
            from: 'suryarajan.selvarajan@gmail.com' // SMTP credentials belong in Jenkins Extended E-mail Notification settings.
          )
        } else {
          emailext (
            to: 'suryarajan.selvarajan@gmail.com',
            subject: subject,
            body: body,
            mimeType: 'text/html',
            from: 'suryarajan.selvarajan@gmail.com'
          )
        }

        echo "Email sent (attempted) - attachments: ${attachments}"
      }
    }

    success { echo "✅ Build passed" }
    unstable { echo "⚠️ Build unstable (tests failed)" }
    failure { echo "❌ Build failed" }
  }
}
