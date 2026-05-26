$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api'
$H = @{ 'Content-Type' = 'application/json; charset=utf-8' }

function Post($path, $obj) {
    $json = $obj | ConvertTo-Json -Compress -Depth 5
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $r = Invoke-RestMethod -Uri "$base/$path" -Method POST -Body $bytes -Headers $H
    return [int]$r.id
}

Write-Host "`n=== USUARIOS ===" -ForegroundColor Cyan
$usuarios = @(
    @{ nome='Pedro Augusto Silva'; email='pedro@nutritrack.com'; dataNascimento='2003-05-12'; telefones=@('(31) 99999-1111','(31) 3333-4444') },
    @{ nome='Luis Felipe Almeida'; email='luis@nutritrack.com'; dataNascimento='2004-08-22'; telefones=@('(31) 98888-2222') },
    @{ nome='Mariana Costa'; email='mariana@nutritrack.com'; dataNascimento='2002-03-15'; telefones=@('(31) 97777-3333','(31) 96666-4444') },
    @{ nome='Joao Pedro Rocha'; email='joao@nutritrack.com'; dataNascimento='2001-11-30'; telefones=@('(31) 95555-5555') },
    @{ nome='Beatriz Lima'; email='beatriz@nutritrack.com'; dataNascimento='2003-07-09'; telefones=@('(31) 94444-6666','(11) 93333-7777') }
)
$idsUsr = @()
foreach ($u in $usuarios) { $id = Post 'usuario' $u; $idsUsr += $id; Write-Host ("  + Usuario id={0}: {1}" -f $id, $u.nome) }

Write-Host "`n=== ALIMENTOS ===" -ForegroundColor Cyan
$alimentos = @(
    @{ nome='Arroz Branco Cozido'; marca='Tio Joao'; kcalPor100g=130; proteinaPor100g=2.7; carboPor100g=28; gorduraPor100g=0.3; tags=@('basico','carboidrato') },
    @{ nome='Feijao Carioca'; marca='Camil'; kcalPor100g=76; proteinaPor100g=4.8; carboPor100g=13.6; gorduraPor100g=0.5; tags=@('basico','proteina','leguminosa') },
    @{ nome='Frango Grelhado'; marca='Sadia'; kcalPor100g=165; proteinaPor100g=31; carboPor100g=0; gorduraPor100g=3.6; tags=@('proteina','carne','magro') },
    @{ nome='Banana Prata'; marca=''; kcalPor100g=89; proteinaPor100g=1.1; carboPor100g=23; gorduraPor100g=0.3; tags=@('fruta','potassio') },
    @{ nome='Pao Frances'; marca='Padaria'; kcalPor100g=300; proteinaPor100g=8; carboPor100g=58; gorduraPor100g=3.1; tags=@('carboidrato','cafe-da-manha') },
    @{ nome='Ovo de Galinha'; marca='Mantiqueira'; kcalPor100g=155; proteinaPor100g=13; carboPor100g=1.1; gorduraPor100g=11; tags=@('proteina','cafe-da-manha') },
    @{ nome='Aveia em Flocos'; marca='Quaker'; kcalPor100g=389; proteinaPor100g=16.9; carboPor100g=66; gorduraPor100g=6.9; tags=@('fibra','cafe-da-manha') },
    @{ nome='Leite Integral'; marca='Itambe'; kcalPor100g=61; proteinaPor100g=3.2; carboPor100g=4.8; gorduraPor100g=3.3; tags=@('laticinio','calcio') },
    @{ nome='Maca Fuji'; marca=''; kcalPor100g=52; proteinaPor100g=0.3; carboPor100g=14; gorduraPor100g=0.2; tags=@('fruta','fibra') },
    @{ nome='Batata Doce Cozida'; marca=''; kcalPor100g=86; proteinaPor100g=1.6; carboPor100g=20; gorduraPor100g=0.1; tags=@('carboidrato','fitness') }
)
$idsAlm = @()
foreach ($a in $alimentos) { $id = Post 'alimento' $a; $idsAlm += $id; Write-Host ("  + Alimento id={0}: {1}" -f $id, $a.nome) }

Write-Host "`n=== REFEICOES ===" -ForegroundColor Cyan
$refeicoes = @(
    @{ usuarioId=$idsUsr[0]; data='2026-04-23'; tipo='Cafe da Manha'; observacao='Cafe reforcado pre-treino' },
    @{ usuarioId=$idsUsr[0]; data='2026-04-23'; tipo='Almoco'; observacao='Refeicao classica brasileira' },
    @{ usuarioId=$idsUsr[0]; data='2026-04-23'; tipo='Lanche da Tarde'; observacao='Lanche leve' },
    @{ usuarioId=$idsUsr[1]; data='2026-04-23'; tipo='Almoco'; observacao='Pos-academia' },
    @{ usuarioId=$idsUsr[2]; data='2026-04-22'; tipo='Jantar'; observacao='Jantar saudavel' },
    @{ usuarioId=$idsUsr[3]; data='2026-04-23'; tipo='Cafe da Manha'; observacao='' }
)
$idsRef = @()
foreach ($r in $refeicoes) { $id = Post 'refeicao' $r; $idsRef += $id; Write-Host ("  + Refeicao id={0}: {1} - {2}" -f $id, $r.tipo, $r.observacao) }

Write-Host "`n=== CONSUMOS ===" -ForegroundColor Cyan
$consumos = @(
    # Cafe da manha do Pedro (refeicao 0)
    @{ refeicaoId=$idsRef[0]; alimentoId=$idsAlm[4]; quantidadeGramas=100 },  # pao
    @{ refeicaoId=$idsRef[0]; alimentoId=$idsAlm[5]; quantidadeGramas=120 },  # ovos
    @{ refeicaoId=$idsRef[0]; alimentoId=$idsAlm[7]; quantidadeGramas=200 },  # leite
    @{ refeicaoId=$idsRef[0]; alimentoId=$idsAlm[3]; quantidadeGramas=120 },  # banana
    # Almoco do Pedro (refeicao 1)
    @{ refeicaoId=$idsRef[1]; alimentoId=$idsAlm[0]; quantidadeGramas=150 },  # arroz
    @{ refeicaoId=$idsRef[1]; alimentoId=$idsAlm[1]; quantidadeGramas=100 },  # feijao
    @{ refeicaoId=$idsRef[1]; alimentoId=$idsAlm[2]; quantidadeGramas=180 },  # frango
    # Lanche do Pedro (refeicao 2)
    @{ refeicaoId=$idsRef[2]; alimentoId=$idsAlm[8]; quantidadeGramas=150 },  # maca
    @{ refeicaoId=$idsRef[2]; alimentoId=$idsAlm[6]; quantidadeGramas=40 },   # aveia
    # Almoco do Luis (refeicao 3)
    @{ refeicaoId=$idsRef[3]; alimentoId=$idsAlm[2]; quantidadeGramas=200 },  # frango
    @{ refeicaoId=$idsRef[3]; alimentoId=$idsAlm[9]; quantidadeGramas=200 },  # batata doce
    @{ refeicaoId=$idsRef[3]; alimentoId=$idsAlm[1]; quantidadeGramas=80 },   # feijao
    # Jantar da Mariana (refeicao 4)
    @{ refeicaoId=$idsRef[4]; alimentoId=$idsAlm[2]; quantidadeGramas=150 },  # frango
    @{ refeicaoId=$idsRef[4]; alimentoId=$idsAlm[9]; quantidadeGramas=150 },  # batata doce
    # Cafe do Joao (refeicao 5)
    @{ refeicaoId=$idsRef[5]; alimentoId=$idsAlm[6]; quantidadeGramas=50 },   # aveia
    @{ refeicaoId=$idsRef[5]; alimentoId=$idsAlm[7]; quantidadeGramas=250 },  # leite
    @{ refeicaoId=$idsRef[5]; alimentoId=$idsAlm[3]; quantidadeGramas=100 }   # banana
)
foreach ($c in $consumos) { $id = Post 'consumo' $c; Write-Host ("  + Consumo id={0}: ref={1} alim={2} {3}g" -f $id, $c.refeicaoId, $c.alimentoId, $c.quantidadeGramas) }

Write-Host "`n=== TOTAIS ===" -ForegroundColor Green
Write-Host ("Usuarios: {0}" -f (Invoke-RestMethod "$base/usuario").Count)
Write-Host ("Alimentos: {0}" -f (Invoke-RestMethod "$base/alimento").Count)
Write-Host ("Refeicoes: {0}" -f (Invoke-RestMethod "$base/refeicao").Count)
Write-Host ("Consumos: {0}" -f (Invoke-RestMethod "$base/consumo").Count)
