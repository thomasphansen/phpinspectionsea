package com.kalessil.phpStorm.phpInspectionsEA.inspectors.codeStyle;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.config.PhpLanguageLevel;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This file is part of the Php Inspections (EA Extended) package.
 *
 * (c) Vladimir Reznichenko <kalessil@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

public class DeprecatedIniOptionsInspector extends BasePhpInspection {
    private static final List<String> targetFunctions = new ArrayList<>();
    static {
        targetFunctions.add("ini_set");
        targetFunctions.add("ini_get");
        targetFunctions.add("ini_alter");
        targetFunctions.add("ini_restore");
    }

    private static final Map<String, Triple<PhpLanguageLevel, PhpLanguageLevel, String>> options = new HashMap<>();
    static {
        /* http://php.net/manual/en/network.configuration.php */
        options.put("define_syslog_variables", Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        /* http://php.net/manual/en/info.configuration.php */
        options.put("magic_quotes_gpc",        Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("magic_quotes_runtime",    Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        /* http://php.net/manual/en/misc.configuration.php */
        options.put("highlight.bg",            Triple.of(null, PhpLanguageLevel.PHP540, null));
        /* http://php.net/manual/en/xsl.configuration.php */
        options.put("xsl.security_prefs",      Triple.of(PhpLanguageLevel.PHP540, PhpLanguageLevel.PHP700, "XsltProcessor->setSecurityPrefs()"));

        /* http://php.net/manual/en/ini.sect.safe-mode.php */
        options.put("safe_mode",                    Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("safe_mode_gid",                Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("safe_mode_include_dir",        Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("safe_mode_exec_dir",           Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("safe_mode_allowed_env_vars",   Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("safe_mode_protected_env_vars", Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));

        /* http://php.net/manual/en/ini.core.php */
        options.put("sql.safe_mode",                  Triple.of(null, PhpLanguageLevel.PHP720, null));
        options.put("asp_tags",                       Triple.of(null, PhpLanguageLevel.PHP700, null));
        options.put("y2k_compliance",                 Triple.of(null, PhpLanguageLevel.PHP540, null));
        options.put("zend.ze1_compatibility_mode",    Triple.of(null, PhpLanguageLevel.PHP530, null));
        options.put("allow_call_time_pass_reference", Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("register_globals",               Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("register_long_arrays",           Triple.of(PhpLanguageLevel.PHP530, PhpLanguageLevel.PHP540, null));
        options.put("always_populate_raw_post_data",  Triple.of(PhpLanguageLevel.PHP560, PhpLanguageLevel.PHP700, null));

        /* http://php.net/manual/en/session.configuration.php */
        options.put("session.hash_function",           Triple.of(null, PhpLanguageLevel.PHP710, null));
        options.put("session.hash_bits_per_character", Triple.of(null, PhpLanguageLevel.PHP710, null));
        options.put("session.entropy_file",            Triple.of(null, PhpLanguageLevel.PHP710, null));
        options.put("session.entropy_length",          Triple.of(null, PhpLanguageLevel.PHP710, null));
        options.put("session.bug_compat_42",           Triple.of(null, PhpLanguageLevel.PHP540, null));
        options.put("session.bug_compat_warn",         Triple.of(null, PhpLanguageLevel.PHP540, null));

        /* http://php.net/manual/en/iconv.configuration.php */
        options.put("iconv.input_encoding",    Triple.of(PhpLanguageLevel.PHP540, null, "default_charset"));
        options.put("iconv.output_encoding",   Triple.of(PhpLanguageLevel.PHP540, null, "default_charset"));
        options.put("iconv.internal_encoding", Triple.of(PhpLanguageLevel.PHP540, null, "default_charset"));

        /* http://php.net/manual/en/mbstring.configuration.php */
        options.put("", Triple.of(, ,));
        deprecations.put("mbstring.func_overload",     PhpLanguageLevel.PHP720);
        deprecations.put("mbstring.http_input",        PhpLanguageLevel.PHP560);
        deprecations.put("mbstring.http_output",       PhpLanguageLevel.PHP560);
        deprecations.put("mbstring.internal_encoding", PhpLanguageLevel.PHP560);
        removals.put("mbstring.script_encoding",       PhpLanguageLevel.PHP540);
        alternatives.put("mbstring.func_overload",     "default_charset");
        alternatives.put("mbstring.http_input",        "default_charset");
        alternatives.put("mbstring.http_output",       "default_charset");
        alternatives.put("mbstring.script_encoding",   "zend.script_encoding");

        /* http://php.net/manual/en/sybase.configuration.php */
        options.put("", Triple.of(, ,));
        deprecations.put("magic_quotes_sybase", PhpLanguageLevel.PHP530);
        removals.put("magic_quotes_sybase",     PhpLanguageLevel.PHP540);
    }

    @NotNull
    public String getShortName() {
        return "DeprecatedIniOptionsInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            @Override
            public void visitPhpFunctionCall(@NotNull final FunctionReference reference) {
                final String functionName = reference.getName();
                if (functionName != null && targetFunctions.contains(functionName)) {
                    final PsiElement[] arguments = reference.getParameters();
                    if (arguments.length > 0 && arguments[0] instanceof StringLiteralExpression) {
                        final String option = ((StringLiteralExpression) arguments[0]).getContents();
                        if (removals.containsKey(option)) {
                            holder.registerProblem(arguments[0], removals.get(option));
                        } else if (deprecations.containsKey(option)) {
                            holder.registerProblem(arguments[0], deprecations.get(option), ProblemHighlightType.LIKE_DEPRECATED);
                        }
                    }
                }
            }
        };
    }
}
