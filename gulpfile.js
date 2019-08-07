let gulp = require('gulp');
const zip = require('gulp-zip');

var minimist = require('minimist');

var knownOptions = {
  string: 'bankId'
};

// temporaryDir;
var options = minimist( process.argv.slice(2), knownOptions );
 
gulp.task('createTemporaryDir', function () {
    return gulp.src([
        `${ options.bankId }/**`,
    ])
        .pipe(gulp.dest( `./temporaryDir/${ options.bankId }/` ))
});

gulp.task('zipAction', function () {
    return gulp.src( `./temporaryDir/**` )
        .pipe(zip( `${ options.bankId }.zip` ))
        .pipe(gulp.dest( './' ));
});

// gulp.task('deleteTemporaryDir', function () { });

gulp.task('beginZip', 
    gulp.series(
        'createTemporaryDir', 
        'zipAction'
    )
);  